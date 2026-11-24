package dev.truewinter.simofa.database;

import com.zaxxer.hikari.HikariDataSource;
import dev.truewinter.simofa.Simofa;
import dev.truewinter.simofa.api.Website;
import dev.truewinter.simofa.api.WebsiteBuild;
import dev.truewinter.simofa.common.BuildStatus;
import dev.truewinter.simofa.common.SimofaLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTransientException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class BuildDatabase {
    private static final int MAX_LOG_BATCH_SIZE = 500;
    private static final int MAX_SYSTEMIC_LOG_RETRY_ATTEMPTS = 5;
    private static final long INITIAL_RETRY_DELAY_MS = 250;
    private static final long MAX_RETRY_DELAY_MS = 30_000;
    private static final long SHUTDOWN_TIMEOUT_MS = 5_000;

    private final HikariDataSource ds;
    private final WebsiteDatabase websiteDatabase;

    private final ExecutorService logWriterExecutor;
    private final Object logLock = new Object();
    private final Object flushLock = new Object();
    private final Deque<PendingLog> pendingLogs = new ArrayDeque<>();
    private final List<PendingLog> retryLogs = new ArrayList<>();
    private int systemicLogRetryAttempts = 0;
    private volatile boolean logWriterRunning = true;

    private final ExecutorService statusWriterExecutor;
    private final Object statusLock = new Object();
    private final Object statusFlushLock = new Object();
    private final Map<String, PendingBuildUpdate> pendingBuildUpdates = new HashMap<>();
    private final Map<String, PendingBuildRecovery> pendingBuildRecoveries = new HashMap<>();
    private volatile boolean statusWriterRunning = true;

    public BuildDatabase(HikariDataSource ds, WebsiteDatabase websiteDatabase) {
        this.ds = ds;
        this.websiteDatabase = websiteDatabase;

        this.logWriterExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "simofa-build-log-writer");
            thread.setDaemon(true);
            return thread;
        });

        this.statusWriterExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "simofa-build-status-writer");
            thread.setDaemon(true);
            return thread;
        });

        this.logWriterExecutor.submit(this::runLogWriter);
        this.statusWriterExecutor.submit(this::runStatusWriter);
    }

    public void addBuild(WebsiteBuild build) throws SQLException {
        try (Connection connection = ds.getConnection()) {
            boolean autoCommit = connection.getAutoCommit();

            try {
                connection.setAutoCommit(false);

                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO `builds` (`id`, `website_id`, `commit_message`, `status`, " +
                                "`queued_at`, `start_time`, `end_time`) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?);")) {
                    statement.setString(1, build.getId());
                    statement.setString(2, build.getWebsite().getId());
                    statement.setString(3, build.getCommit() == null ? "" : build.getCommit());
                    statement.setString(4, build.getStatus().toString());
                    statement.setLong(5, build.getQueuedAt());
                    statement.setLong(6, build.getStartTime());
                    statement.setLong(7, build.getEndTime());

                    statement.execute();
                }

                for (SimofaLog log : new ArrayList<>(build.getLogs())) {
                    insertLog(connection, build.getId(), log);
                }

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                try {
                    connection.setAutoCommit(autoCommit);
                } catch (SQLException e) {
                    Simofa.getLogger().warn("Failed to restore auto-commit state after saving build", e);
                }
            }
        }
    }

    public void queueBuildUpdate(WebsiteBuild build) {
        synchronized (statusLock) {
            pendingBuildUpdates.put(build.getId(), new PendingBuildUpdate(
                    build.getId(),
                    build.getStatus(),
                    build.getStartTime(),
                    build.getEndTime()
            ));
            statusLock.notifyAll();
        }
    }

    public void queueBuildRecovery(WebsiteBuild build, SimofaLog log) {
        synchronized (statusLock) {
            pendingBuildRecoveries.put(build.getId(), new PendingBuildRecovery(
                    build.getId(),
                    build.getStatus(),
                    build.getStartTime(),
                    build.getEndTime(),
                    log
            ));
            statusLock.notifyAll();
        }
    }

    public void queueLog(String buildId, SimofaLog log) {
        synchronized (logLock) {
            pendingLogs.addLast(new PendingLog(buildId, log));
            logLock.notifyAll();
        }
    }

    public void close() {
        long deadline = System.currentTimeMillis() + SHUTDOWN_TIMEOUT_MS;

        logWriterRunning = false;
        synchronized (logLock) {
            logLock.notifyAll();
        }
        logWriterExecutor.shutdownNow();

        statusWriterRunning = false;
        synchronized (statusLock) {
            statusLock.notifyAll();
        }
        statusWriterExecutor.shutdownNow();

        boolean logWriterStopped = awaitTermination(logWriterExecutor, deadline);
        boolean statusWriterStopped = awaitTermination(statusWriterExecutor, deadline);

        ExecutorService finalDrain = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "simofa-build-persistence-drain");
            thread.setDaemon(true);
            return thread;
        });

        Future<Boolean> future = finalDrain.submit(() -> {
            while (System.currentTimeMillis() < deadline) {
                boolean logsFlushed = flushPendingLogsUntil(deadline);
                boolean updatesFlushed = flushPendingBuildUpdatesUntil(deadline);

                if (logsFlushed && updatesFlushed) {
                    return true;
                }

                if (System.currentTimeMillis() >= deadline) {
                    return false;
                }

                try {
                    Thread.sleep(INITIAL_RETRY_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }

            return false;
        });

        boolean drained = false;

        try {
            long remaining = Math.max(1, deadline - System.currentTimeMillis());
            drained = Boolean.TRUE.equals(future.get(remaining, TimeUnit.MILLISECONDS));
        } catch (TimeoutException e) {
            future.cancel(true);
            Simofa.getLogger().warn("Timed out draining build persistence during shutdown");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            Simofa.getLogger().warn("Failed to drain build persistence during shutdown", e.getCause());
        } finally {
            finalDrain.shutdownNow();
        }

        if (!logWriterStopped || !statusWriterStopped || !drained
                || !isLogQueueEmpty() || !isStatusQueueEmpty()) {
            Simofa.getLogger().warn("Shutdown completed without confirming all build logs and status updates were saved");
        }
    }

    private boolean awaitTermination(ExecutorService executor, long deadline) {
        long remaining = deadline - System.currentTimeMillis();
        if (remaining <= 0) {
            return false;
        }

        try {
            return executor.awaitTermination(remaining, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void runLogWriter() {
        long delay = INITIAL_RETRY_DELAY_MS;

        while (logWriterRunning) {
            try {
                synchronized (logLock) {
                    while (logWriterRunning && pendingLogs.isEmpty() && retryLogs.isEmpty()) {
                        logLock.wait();
                    }
                }

                if (!logWriterRunning) {
                    break;
                }

                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            if (attemptFlushLogs()) {
                delay = INITIAL_RETRY_DELAY_MS;
            } else {
                delay = Math.min(delay * 2, MAX_RETRY_DELAY_MS);
            }
        }
    }

    private boolean attemptFlushLogs() {
        synchronized (flushLock) {
            return flushOneLogBatch();
        }
    }

    private boolean flushOneLogBatch() {
        List<PendingLog> batch;
        boolean retrying;

        synchronized (logLock) {
            if (!retryLogs.isEmpty()) {
                batch = new ArrayList<>(retryLogs);
                retrying = true;
            } else {
                if (pendingLogs.isEmpty()) {
                    return true;
                }

                int count = Math.min(pendingLogs.size(), MAX_LOG_BATCH_SIZE);
                batch = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    batch.add(pendingLogs.removeFirst());
                }
                retrying = false;
            }
        }

        try {
            insertLogs(batch);

            synchronized (logLock) {
                systemicLogRetryAttempts = 0;

                if (retrying) {
                    retryLogs.clear();
                }
            }

            return true;
        } catch (Exception e) {
            boolean dropBatch = false;

            if (e instanceof SQLException sqlException) {
                if (!isKnownTransient(sqlException)) {
                    synchronized (logLock) {
                        systemicLogRetryAttempts++;

                        if (systemicLogRetryAttempts >= MAX_SYSTEMIC_LOG_RETRY_ATTEMPTS) {
                            systemicLogRetryAttempts = 0;
                            dropBatch = true;
                        }
                    }
                } else {
                    synchronized (logLock) {
                        systemicLogRetryAttempts = 0;
                    }
                }
            } else {
                synchronized (logLock) {
                    systemicLogRetryAttempts = 0;
                }
            }

            synchronized (logLock) {
                if (dropBatch) {
                    retryLogs.clear();
                } else if (!retrying) {
                    retryLogs.addAll(batch);
                }
            }

            if (dropBatch) {
                Simofa.getLogger().error(String.format(
                        "Dropping %d build log entries after %d consecutive systemic persistence failures",
                        batch.size(), MAX_SYSTEMIC_LOG_RETRY_ATTEMPTS
                ), e);
            } else {
                Simofa.getLogger().error("Failed to save build logs to database, will retry", e);
            }

            return false;
        }
    }

    private boolean flushPendingLogsUntil(long deadline) {
        synchronized (flushLock) {
            while (System.currentTimeMillis() < deadline) {
                synchronized (logLock) {
                    if (pendingLogs.isEmpty() && retryLogs.isEmpty()) {
                        return true;
                    }
                }

                if (!flushOneLogBatch()) {
                    return false;
                }
            }

            return isLogQueueEmpty();
        }
    }

    private boolean isLogQueueEmpty() {
        synchronized (logLock) {
            return pendingLogs.isEmpty() && retryLogs.isEmpty();
        }
    }

    private void insertLogs(List<PendingLog> logs) throws SQLException {
        if (logs.isEmpty()) {
            return;
        }

        try {
            insertLogsBatch(logs);
        } catch (SQLException e) {
            if (!isPermanentRowFailure(e)) {
                throw e;
            }

            Simofa.getLogger().warn("Batch build log insert failed, isolating permanently rejected rows", e);
            insertLogsIndividually(logs);
        }
    }

    private void insertLogsBatch(List<PendingLog> logs) throws SQLException {
        try (Connection connection = ds.getConnection()) {
            boolean autoCommit = connection.getAutoCommit();

            try {
                connection.setAutoCommit(false);

                try (PreparedStatement statement = connection.prepareStatement(insertLogSql())) {
                    for (PendingLog log : logs) {
                        setLogParameters(statement, log.buildId(), log.log());
                        statement.addBatch();
                    }

                    statement.executeBatch();
                }

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                try {
                    connection.setAutoCommit(autoCommit);
                } catch (SQLException e) {
                    Simofa.getLogger().warn("Failed to restore auto-commit state", e);
                }
            }
        }
    }

    private void insertLogsIndividually(List<PendingLog> logs) throws SQLException {
        try (Connection connection = ds.getConnection()) {
            boolean autoCommit = connection.getAutoCommit();

            try {
                connection.setAutoCommit(true);

                try (PreparedStatement statement = connection.prepareStatement(insertLogSql())) {
                    for (PendingLog log : logs) {
                        try {
                            setLogParameters(statement, log.buildId(), log.log());
                            statement.executeUpdate();
                        } catch (SQLException e) {
                            if (!isPermanentRowFailure(e)) {
                                throw e;
                            }

                            Simofa.getLogger().warn(String.format(
                                    "Discarding build log %s for build %s",
                                    log.log().getUuid(), log.buildId()
                            ), e);
                        }
                    }
                }
            } finally {
                try {
                    connection.setAutoCommit(autoCommit);
                } catch (SQLException e) {
                    Simofa.getLogger().warn("Failed to restore auto-commit state", e);
                }
            }
        }
    }

    private void insertLog(Connection connection, String buildId, SimofaLog log) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(insertLogSql())) {
            setLogParameters(statement, buildId, log);
            statement.executeUpdate();
        }
    }

    private String insertLogSql() {
        return "INSERT INTO `build_logs` (`build_id`, `type`, `message`, `uuid`, `timestamp`) " +
                "VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE `id` = `id`;";
    }

    private void setLogParameters(PreparedStatement statement, String buildId, SimofaLog log) throws SQLException {
        statement.setString(1, buildId);
        statement.setString(2, log.getType() == null ? "" : log.getType());
        statement.setString(3, log.getLog() == null ? "" : log.getLog());
        statement.setString(4, log.getUuid());
        statement.setLong(5, log.getTimestamp());
    }

    private boolean isKnownTransient(SQLException e) {
        if (e instanceof SQLTransientException) {
            return true;
        }

        String sqlState = e.getSQLState();
        if (sqlState != null && (sqlState.startsWith("08") || sqlState.startsWith("40")
                || sqlState.equals("HYT00") || sqlState.equals("HYT01"))) {
            return true;
        }

        int errorCode = e.getErrorCode();
        return errorCode == 1205 || errorCode == 1213 || errorCode == 1290;
    }

    private boolean isPermanentRowFailure(SQLException e) {
        String sqlState = e.getSQLState();
        if (sqlState == null) {
            return false;
        }

        if (sqlState.startsWith("22")) {
            return true;
        }

        if ("23000".equals(sqlState)) {
            int errorCode = e.getErrorCode();
            return errorCode == 1048 || errorCode == 1062 || errorCode == 1452;
        }

        return false;
    }

    private void runStatusWriter() {
        long delay = INITIAL_RETRY_DELAY_MS;

        while (statusWriterRunning) {
            try {
                synchronized (statusLock) {
                    while (statusWriterRunning
                            && pendingBuildUpdates.isEmpty() && pendingBuildRecoveries.isEmpty()) {
                        statusLock.wait();
                    }
                }

                if (!statusWriterRunning) {
                    break;
                }

                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            boolean updatesFlushed = flushPendingStatusUpdatesOnce();
            boolean recoveriesFlushed = flushPendingRecoveriesOnce();

            if (updatesFlushed && recoveriesFlushed) {
                delay = INITIAL_RETRY_DELAY_MS;
            } else {
                delay = Math.min(delay * 2, MAX_RETRY_DELAY_MS);
            }
        }
    }

    private boolean flushPendingStatusUpdatesOnce() {
        synchronized (statusFlushLock) {
            return flushOneStatusBatch();
        }
    }

    private boolean flushOneStatusBatch() {
        Collection<PendingBuildUpdate> batch;

        synchronized (statusLock) {
            if (pendingBuildUpdates.isEmpty()) {
                return true;
            }

            batch = new ArrayList<>(pendingBuildUpdates.values());
            pendingBuildUpdates.clear();
        }

        try {
            updateBuilds(batch);
            return true;
        } catch (Exception e) {
            synchronized (statusLock) {
                for (PendingBuildUpdate update : batch) {
                    pendingBuildUpdates.putIfAbsent(update.buildId(), update);
                }
            }

            Simofa.getLogger().error("Failed to save build statuses to database, will retry", e);
            return false;
        }
    }

    private boolean flushPendingRecoveriesOnce() {
        synchronized (statusFlushLock) {
            return flushOneRecoveryBatch();
        }
    }

    private boolean flushOneRecoveryBatch() {
        Collection<PendingBuildRecovery> batch;

        synchronized (statusLock) {
            if (pendingBuildRecoveries.isEmpty()) {
                return true;
            }

            batch = new ArrayList<>(pendingBuildRecoveries.values());
            pendingBuildRecoveries.clear();
        }

        try {
            for (PendingBuildRecovery recovery : batch) {
                persistBuildRecovery(recovery);
            }

            return true;
        } catch (Exception e) {
            synchronized (statusLock) {
                for (PendingBuildRecovery recovery : batch) {
                    pendingBuildRecoveries.putIfAbsent(recovery.buildId(), recovery);
                }
            }

            Simofa.getLogger().error("Failed to save interrupted builds to database, will retry", e);
            return false;
        }
    }

    private boolean flushPendingBuildUpdatesUntil(long deadline) {
        synchronized (statusFlushLock) {
            while (System.currentTimeMillis() < deadline) {
                synchronized (statusLock) {
                    if (pendingBuildUpdates.isEmpty() && pendingBuildRecoveries.isEmpty()) {
                        return true;
                    }
                }

                boolean updatesFlushed = flushOneStatusBatch();
                boolean recoveriesFlushed = flushOneRecoveryBatch();

                if (!updatesFlushed || !recoveriesFlushed) {
                    return false;
                }
            }

            return isStatusQueueEmpty();
        }
    }

    private boolean isStatusQueueEmpty() {
        synchronized (statusLock) {
            return pendingBuildUpdates.isEmpty() && pendingBuildRecoveries.isEmpty();
        }
    }

    private void updateBuilds(Collection<PendingBuildUpdate> updates) throws SQLException {
        if (updates.isEmpty()) {
            return;
        }

        try (Connection connection = ds.getConnection()) {
            boolean autoCommit = connection.getAutoCommit();

            try {
                connection.setAutoCommit(false);

                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE `builds` SET `status` = ?, `start_time` = ?, `end_time` = ? " +
                                "WHERE `id` = ?;")) {
                    for (PendingBuildUpdate update : updates) {
                        statement.setString(1, update.status().toString());
                        statement.setLong(2, update.startTime());
                        statement.setLong(3, update.endTime());
                        statement.setString(4, update.buildId());
                        statement.addBatch();
                    }

                    statement.executeBatch();
                }

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                try {
                    connection.setAutoCommit(autoCommit);
                } catch (SQLException e) {
                    Simofa.getLogger().warn("Failed to restore auto-commit state", e);
                }
            }
        }
    }

    private void persistBuildRecovery(PendingBuildRecovery recovery) throws SQLException {
        try (Connection connection = ds.getConnection()) {
            boolean autoCommit = connection.getAutoCommit();

            try {
                connection.setAutoCommit(false);

                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE `builds` SET `status` = ?, `start_time` = ?, `end_time` = ? " +
                                "WHERE `id` = ?;")) {
                    statement.setString(1, recovery.status().toString());
                    statement.setLong(2, recovery.startTime());
                    statement.setLong(3, recovery.endTime());
                    statement.setString(4, recovery.buildId());
                    statement.execute();
                }

                insertLog(connection, recovery.buildId(), recovery.log());
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                try {
                    connection.setAutoCommit(autoCommit);
                } catch (SQLException e) {
                    Simofa.getLogger().warn("Failed to restore auto-commit state", e);
                }
            }
        }
    }

    public List<WebsiteBuild> getRecentBuilds(int limitPerWebsite) throws SQLException {
        Map<String, Website> websites = new HashMap<>();
        for (Website website : websiteDatabase.getWebsites()) {
            websites.put(website.getId(), website);
        }

        if (websites.isEmpty()) {
            return new ArrayList<>();
        }

        List<BuildRow> rows = new ArrayList<>();

        try (Connection connection = ds.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT `id`, `website_id`, `commit_message`, `status`, `queued_at`, " +
                            "`start_time`, `end_time` FROM (" +
                            "SELECT `id`, `website_id`, `commit_message`, `status`, `queued_at`, " +
                            "`start_time`, `end_time`, " +
                            "ROW_NUMBER() OVER (PARTITION BY `website_id` ORDER BY `id` DESC) AS `rn` " +
                            "FROM `builds`) AS `ranked` WHERE `rn` <= ? " +
                            "ORDER BY `website_id` ASC, `id` ASC;")) {
                statement.setInt(1, limitPerWebsite);
                ResultSet rs = statement.executeQuery();

                while (rs.next()) {
                    Website website = websites.get(rs.getString("website_id"));
                    if (website == null) {
                        continue;
                    }

                    String buildId = rs.getString("id");
                    Optional<BuildStatus> status = parseStatus(rs.getString("status"), buildId);
                    if (status.isEmpty()) {
                        continue;
                    }

                    rows.add(new BuildRow(
                            buildId,
                            website,
                            rs.getString("commit_message"),
                            status.get(),
                            rs.getLong("queued_at"),
                            rs.getLong("start_time"),
                            rs.getLong("end_time")
                    ));
                }
            }

            if (rows.isEmpty()) {
                return new ArrayList<>();
            }

            List<String> buildIds = new ArrayList<>(rows.size());
            for (BuildRow row : rows) {
                buildIds.add(row.id());
            }

            Map<String, List<SimofaLog>> logs = getLogsForBuilds(connection, buildIds);
            List<WebsiteBuild> builds = new ArrayList<>(rows.size());

            for (BuildRow row : rows) {
                builds.add(WebsiteBuild.fromDatabase(
                        row.id(),
                        row.website(),
                        row.commit(),
                        row.status(),
                        row.queuedAt(),
                        row.startTime(),
                        row.endTime(),
                        logs.getOrDefault(row.id(), new ArrayList<>()),
                        null
                ));
            }

            return builds;
        }
    }

    public List<WebsiteBuild> getBuilds(String websiteId, int limit) throws SQLException {
        Optional<Website> website = websiteDatabase.getWebsiteById(websiteId);
        if (website.isEmpty()) {
            return new ArrayList<>();
        }

        List<WebsiteBuild> builds = new ArrayList<>();

        try (Connection connection = ds.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT `id`, `website_id`, `commit_message`, `status`, `queued_at`, " +
                            "`start_time`, `end_time` FROM `builds` " +
                            "WHERE `website_id` = ? ORDER BY `id` DESC LIMIT ?;")) {
                statement.setString(1, websiteId);
                statement.setInt(2, limit);
                ResultSet rs = statement.executeQuery();

                while (rs.next()) {
                    getBuildFromResultSet(rs, website.get()).ifPresent(builds::add);
                }
            }
        }

        Collections.reverse(builds);
        return builds;
    }

    public Optional<WebsiteBuild> getBuild(String websiteId, String buildId) throws SQLException {
        Optional<Website> website = websiteDatabase.getWebsiteById(websiteId);
        if (website.isEmpty()) {
            return Optional.empty();
        }

        try (Connection connection = ds.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT `id`, `website_id`, `commit_message`, `status`, `queued_at`, " +
                            "`start_time`, `end_time` FROM `builds` " +
                            "WHERE `website_id` = ? AND `id` = ?;")) {
                statement.setString(1, websiteId);
                statement.setString(2, buildId);
                ResultSet rs = statement.executeQuery();

                if (rs.next()) {
                    return getBuildFromResultSet(rs, website.get());
                }
            }
        }

        return Optional.empty();
    }

    private Optional<WebsiteBuild> getBuildFromResultSet(ResultSet rs, Website website) throws SQLException {
        String buildId = rs.getString("id");
        Optional<BuildStatus> status = parseStatus(rs.getString("status"), buildId);
        if (status.isEmpty()) {
            return Optional.empty();
        }

        List<SimofaLog> logs = getLogs(buildId, 0);

        return Optional.of(WebsiteBuild.fromDatabase(
                buildId,
                website,
                rs.getString("commit_message"),
                status.get(),
                rs.getLong("queued_at"),
                rs.getLong("start_time"),
                rs.getLong("end_time"),
                logs,
                null
        ));
    }

    private Optional<BuildStatus> parseStatus(String status, String buildId) {
        if (status == null) {
            Simofa.getLogger().warn(String.format("Ignoring build %s with missing status", buildId));
            return Optional.empty();
        }

        try {
            return Optional.of(BuildStatus.valueOf(status));
        } catch (IllegalArgumentException e) {
            Simofa.getLogger().warn(String.format("Ignoring build %s with unknown status '%s'", buildId, status));
            return Optional.empty();
        }
    }

    private Map<String, List<SimofaLog>> getLogsForBuilds(Connection connection, List<String> buildIds) throws SQLException {
        Map<String, List<SimofaLog>> logs = new HashMap<>();

        for (int start = 0; start < buildIds.size(); start += MAX_LOG_BATCH_SIZE) {
            List<String> chunk = buildIds.subList(start, Math.min(start + MAX_LOG_BATCH_SIZE, buildIds.size()));
            String placeholders = String.join(",", Collections.nCopies(chunk.size(), "?"));

            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT `build_id`, `type`, `message`, `uuid`, `timestamp` FROM `build_logs` " +
                            "WHERE `build_id` IN (" + placeholders + ") " +
                            "ORDER BY `build_id` ASC, `timestamp` ASC, `id` ASC;")) {
                for (int i = 0; i < chunk.size(); i++) {
                    statement.setString(i + 1, chunk.get(i));
                }

                ResultSet rs = statement.executeQuery();

                while (rs.next()) {
                    logs.computeIfAbsent(rs.getString("build_id"), k -> new ArrayList<>())
                            .add(new SimofaLog(
                                    rs.getString("type"),
                                    rs.getString("message"),
                                    rs.getString("uuid"),
                                    rs.getLong("timestamp")
                            ));
                }
            }
        }

        return logs;
    }

    public List<SimofaLog> getLogs(String buildId, long afterTimestamp) throws SQLException {
        List<SimofaLog> logs = new ArrayList<>();

        try (Connection connection = ds.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT `type`, `message`, `uuid`, `timestamp` FROM `build_logs` " +
                            "WHERE `build_id` = ? AND `timestamp` > ? ORDER BY `timestamp` ASC, `id` ASC;")) {
                statement.setString(1, buildId);
                statement.setLong(2, afterTimestamp);
                ResultSet rs = statement.executeQuery();

                while (rs.next()) {
                    logs.add(new SimofaLog(
                            rs.getString("type"),
                            rs.getString("message"),
                            rs.getString("uuid"),
                            rs.getLong("timestamp")
                    ));
                }
            }
        }

        return logs;
    }

    public void deleteBuild(String buildId) throws SQLException {
        try (Connection connection = ds.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM `builds` WHERE `id` = ?;")) {
                statement.setString(1, buildId);
                statement.execute();
            }
        }
    }

    public void pruneBuilds(String websiteId, int keep) throws SQLException {
        List<String> buildIds = new ArrayList<>();

        try (Connection connection = ds.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT `id` FROM `builds` WHERE `website_id` = ? ORDER BY `id` DESC;")) {
                statement.setString(1, websiteId);
                ResultSet rs = statement.executeQuery();

                while (rs.next()) {
                    buildIds.add(rs.getString("id"));
                }
            }

            if (buildIds.size() <= keep) {
                return;
            }

            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM `builds` WHERE `id` = ?;")) {
                for (int i = keep; i < buildIds.size(); i++) {
                    statement.setString(1, buildIds.get(i));
                    statement.addBatch();
                }

                statement.executeBatch();
            }
        }
    }

    private record PendingLog(String buildId, SimofaLog log) {}

    private record PendingBuildUpdate(String buildId, BuildStatus status, long startTime, long endTime) {}

    private record PendingBuildRecovery(String buildId, BuildStatus status, long startTime, long endTime,
                                        SimofaLog log) {}

    private record BuildRow(String id, Website website, String commit, BuildStatus status,
                            long queuedAt, long startTime, long endTime) {}
}
