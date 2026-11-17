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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BuildDatabase {
    private static final int MAX_LOG_BATCH_SIZE = 500;
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
    private volatile boolean running = true;

    public BuildDatabase(HikariDataSource ds, WebsiteDatabase websiteDatabase) {
        this.ds = ds;
        this.websiteDatabase = websiteDatabase;
        this.logWriterExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "simofa-build-log-writer");
            thread.setDaemon(true);
            return thread;
        });
        this.logWriterExecutor.submit(this::runLogWriter);
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
                    addLog(connection, build.getId(), log);
                }

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        }
    }

    public void updateBuild(WebsiteBuild build) throws SQLException {
        try (Connection connection = ds.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE `builds` SET `status` = ?, `start_time` = ?, `end_time` = ? " +
                            "WHERE `id` = ?;")) {
                statement.setString(1, build.getStatus().toString());
                statement.setLong(2, build.getStartTime());
                statement.setLong(3, build.getEndTime());
                statement.setString(4, build.getId());
                statement.execute();
            }
        }
    }

    public void queueLog(String buildId, SimofaLog log) {
        synchronized (logLock) {
            pendingLogs.addLast(new PendingLog(buildId, log));
            logLock.notifyAll();
        }
    }

    /**
     * Attempts to save all queued logs immediately.
     *
     * @return true if all pending logs were saved, false if they will be retried later
     */
    public boolean flushPendingLogs() {
        while (true) {
            synchronized (logLock) {
                if (pendingLogs.isEmpty() && retryLogs.isEmpty()) {
                    return true;
                }
            }

            if (!attemptFlush()) {
                return false;
            }
        }
    }

    public void close() {
        running = false;
        synchronized (logLock) {
            logLock.notifyAll();
        }
        logWriterExecutor.shutdownNow();

        try {
            if (!logWriterExecutor.awaitTermination(SHUTDOWN_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                Simofa.getLogger().warn("Timed out waiting for build log writer to stop");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long deadline = System.currentTimeMillis() + SHUTDOWN_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            synchronized (logLock) {
                if (pendingLogs.isEmpty() && retryLogs.isEmpty()) {
                    return;
                }
            }

            if (!attemptFlush()) {
                try {
                    Thread.sleep(INITIAL_RETRY_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        synchronized (logLock) {
            if (!pendingLogs.isEmpty() || !retryLogs.isEmpty()) {
                Simofa.getLogger().warn("Some build logs could not be saved before shutdown");
            }
        }
    }

    private void runLogWriter() {
        long delay = INITIAL_RETRY_DELAY_MS;

        while (running) {
            try {
                synchronized (logLock) {
                    while (running && pendingLogs.isEmpty() && retryLogs.isEmpty()) {
                        logLock.wait();
                    }
                }

                if (!running) {
                    break;
                }

                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            if (attemptFlush()) {
                delay = INITIAL_RETRY_DELAY_MS;
            } else {
                delay = Math.min(delay * 2, MAX_RETRY_DELAY_MS);
            }
        }
    }

    private boolean attemptFlush() {
        synchronized (flushLock) {
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

                if (retrying) {
                    synchronized (logLock) {
                        retryLogs.clear();
                    }
                }

                return true;
            } catch (Exception e) {
                if (!retrying) {
                    synchronized (logLock) {
                        retryLogs.addAll(batch);
                    }
                }

                Simofa.getLogger().error("Failed to save build logs to database, will retry", e);
                return false;
            }
        }
    }

    private void insertLogs(List<PendingLog> logs) throws SQLException {
        if (logs.isEmpty()) {
            return;
        }

        try (Connection connection = ds.getConnection()) {
            boolean autoCommit = connection.getAutoCommit();

            try {
                connection.setAutoCommit(false);

                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO `build_logs` (`build_id`, `type`, `message`, `uuid`, `timestamp`) " +
                                "VALUES (?, ?, ?, ?, ?);")) {
                    for (PendingLog log : logs) {
                        statement.setString(1, log.buildId());
                        statement.setString(2, log.log().getType() == null ? "" : log.log().getType());
                        statement.setString(3, log.log().getLog() == null ? "" : log.log().getLog());
                        statement.setString(4, log.log().getUuid());
                        statement.setLong(5, log.log().getTimestamp());
                        statement.addBatch();
                    }

                    statement.executeBatch();
                }

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        }
    }

    private void addLog(Connection connection, String buildId, SimofaLog log) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO `build_logs` (`build_id`, `type`, `message`, `uuid`, `timestamp`) " +
                        "VALUES (?, ?, ?, ?, ?);")) {
            statement.setString(1, buildId);
            statement.setString(2, log.getType() == null ? "" : log.getType());
            statement.setString(3, log.getLog() == null ? "" : log.getLog());
            statement.setString(4, log.getUuid());
            statement.setLong(5, log.getTimestamp());
            statement.execute();
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

    private record BuildRow(String id, Website website, String commit, BuildStatus status,
                            long queuedAt, long startTime, long endTime) {}
}
