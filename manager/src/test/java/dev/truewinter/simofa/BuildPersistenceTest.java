package dev.truewinter.simofa;

import dev.truewinter.simofa.api.SimofaPluginManager;
import dev.truewinter.simofa.api.Website;
import dev.truewinter.simofa.api.WebsiteBuild;
import dev.truewinter.simofa.common.BuildStatus;
import dev.truewinter.simofa.common.SimofaLog;
import dev.truewinter.simofa.config.Config;
import dev.truewinter.simofa.database.BuildDatabase;
import dev.truewinter.simofa.database.Database;
import dev.truewinter.simofa.docker.BuildQueue;
import dev.truewinter.simofa.docker.BuildQueueManager;
import dev.truewinter.simofa.docker.WebsiteBuilder;
import dev.truewinter.simofa.migrator.migrations.AddBuildsTables_2026111601;
import dev.truewinter.simofa.routes.Route;
import dev.truewinter.simofa.routes.webhook.DeployHookRoute;
import dev.truewinter.simofa.routes.webhook.GithubAppWebhookRoute;
import io.javalin.http.Context;
import io.javalin.http.InternalServerErrorResponse;

import java.lang.reflect.Proxy;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

// Run main in a fresh JVM with the manager module's test runtime classpath.
// Set SIMOFA_TEST_MYSQL_DATABASE and SIMOFA_TEST_MYSQL_USER for a disposable,
// empty MySQL 8 schema on localhost:3306; SIMOFA_TEST_MYSQL_PASSWORD is optional.
// This test creates and populates tables in that schema.
public class BuildPersistenceTest {
    private static final String INTERRUPTED = "Build interrupted by server restart";

    public static void main(String[] args) throws Exception {
        TestConfig config = new TestConfig();
        requireEmptySchema(config);

        String primaryId;
        String secondaryId;
        String interruptedId;
        Database first = new Database(config);
        try {
            checkAdmissionRollsBackWhenInitialLogFails(first);
            checkMigrationCanRecreateTables(first);

            primaryId = first.getWebsiteDatabase().addWebsite(website(null, "primary"));
            secondaryId = first.getWebsiteDatabase().addWebsite(website(null, "secondary"));
            Website primary = first.getWebsiteDatabase().getWebsiteById(primaryId).orElseThrow();
            Website secondary = first.getWebsiteDatabase().getWebsiteById(secondaryId).orElseThrow();
            for (int i = 1; i <= 6; i++) {
                first.getBuildDatabase().addBuild(historicalBuild(primary, i,
                        i == 6 ? BuildStatus.BUILDING : BuildStatus.DEPLOYED));
            }
            first.getBuildDatabase().addBuild(historicalBuild(secondary, 7, BuildStatus.DEPLOYED));
            interruptedId = historicalId(6);
        } finally {
            first.close();
        }

        WebsiteBuild recovered;
        WebsiteBuild stopped;
        Database restarted = new Database(config);
        try {
            API api = new API(restarted, null);
            SimofaPluginManager.getInstance(api);
            Route.init(config, restarted);
            BuildQueueManager manager = BuildQueueManager.getInstance(config, restarted);
            // Supply the singleton normally installed by server startup, without starting its timers or Docker.
            var field = Simofa.class.getDeclaredField("buildQueueManager");
            field.setAccessible(true);
            field.set(null, manager);
            BuildQueue queue = manager.getBuildQueue();
            Website primary = restarted.getWebsiteDatabase().getWebsiteById(primaryId).orElseThrow();
            Website secondary = restarted.getWebsiteDatabase().getWebsiteById(secondaryId).orElseThrow();

            recovered = checkHistoryRestoration(queue, primaryId, secondaryId, interruptedId);
            stopped = checkQueueAdmissionAndShutdown(api, queue, restarted.getBuildDatabase(), primary, secondary);
            checkWebhookRejection(primary);
        } finally {
            restarted.close();
        }

        Database reopened = new Database(config);
        try {
            checkWritesSurviveShutdown(reopened.getBuildDatabase(), primaryId, secondaryId, recovered, stopped);
        } finally {
            reopened.close();
        }
    }

    private static void checkAdmissionRollsBackWhenInitialLogFails(Database database) throws Exception {
        String id = database.getWebsiteDatabase().addWebsite(website(null, "rollback"));
        Website site = database.getWebsiteDatabase().getWebsiteById(id).orElseThrow();
        WebsiteBuild invalid = new WebsiteBuild(site, "log write must fail", null);
        invalid.addLog(new SimofaLog("ERROR", "missing log UUID", null, 1));

        expect(SQLException.class, () -> database.getBuildDatabase().addBuild(invalid));
        check(database.getBuildDatabase().getBuild(id, invalid.getId()).isEmpty(),
                "A build row survived a failed insert of its initial logs");
        check(database.getBuildDatabase().getLogs(invalid.getId(), 0).isEmpty(),
                "A log survived the rolled back build insertion");
        database.getWebsiteDatabase().deleteWebsite(id);
    }

    private static void checkMigrationCanRecreateTables(Database database) throws Exception {
        // The fresh schema has no build data at this point.
        var migration = new AddBuildsTables_2026111601() {
            @Override
            protected Database getDatabase() {
                return database;
            }
        };
        migration.down();
        expect(SQLException.class, () -> database.getBuildDatabase().getLogs("missing", 0));
        migration.up();
        migration.up();
        check(database.getBuildDatabase().getLogs("missing", 0).isEmpty(),
                "The migration should create a readable empty log table and be safe to reapply");
    }

    private static WebsiteBuild checkHistoryRestoration(BuildQueue queue, String primaryId, String secondaryId,
                                                       String interruptedId) {
        check(queue.loadBuilds() && queue.loadBuilds(), "History load and repeat load should both succeed");
        Map<String, List<WebsiteBuild>> history = queue.getWebsiteBuildList();
        check(history.get(primaryId).stream().map(WebsiteBuild::getCommit).toList()
                        .equals(List.of("commit 2 🚀", "commit 3 🚀", "commit 4 🚀", "commit 5 🚀", "commit 6 🚀")),
                "History should load the newest five builds in chronological order without duplicates");
        WebsiteBuild completed = history.get(secondaryId).get(0);
        check(history.get(secondaryId).size() == 1 && completed.getStatus() == BuildStatus.DEPLOYED
                && completed.getEndTime() == 30 && completed.getLogs().size() == 2,
                "Loading one site's history must preserve the other site's completed build");

        WebsiteBuild recovered = history.get(primaryId).get(4);
        check(recovered.getId().equals(interruptedId) && recovered.getStatus() == BuildStatus.ERROR
                && recovered.getStartTime() == 20 && recovered.getEndTime() > 20
                && messages(recovered.getLogs()).equals(List.of("first 🚀", "second", INTERRUPTED)),
                "Interrupted build must keep its logs and acquire one recovery diagnostic");
        check(queue.nextIfCapacityAvailable(2).isEmpty(), "Restored history must never be scheduled");

        history.get(primaryId).clear();
        history.clear();
        check(queue.getWebsiteBuildList().get(primaryId).size() == 5, "History snapshots must not mutate the queue");
        return recovered;
    }

    private static WebsiteBuild checkQueueAdmissionAndShutdown(API api, BuildQueue queue, BuildDatabase database,
                                                               Website primary, Website secondary) throws SQLException {
        Website missing = website("00000000-0000-0000-0000-000000000099", "missing");
        check(api.triggerBuild(missing, "[NO CI] intentional skip") && queue.getBuildQueue().isEmpty(),
                "An intentional skip is accepted without creating a build");
        check(!api.triggerBuild(missing) && queue.getBuildQueue().isEmpty()
                        && !queue.getWebsiteBuildList().containsKey(missing.getId()),
                "A foreign key rejection must not publish a queued build");

        check(api.triggerBuild(primary, false), "API should accept a valid build");
        check(queue.nextIfCapacityAvailable(0).isEmpty(), "Zero capacity must leave the queued build untouched");
        WebsiteBuild reserved = queue.nextIfCapacityAvailable(1).orElseThrow();
        check(reserved.getCommit().equals("[no cache] <manual build>"), "API lost the no-cache option");
        WebsiteBuild persisted = database.getBuild(primary.getId(), reserved.getId()).orElseThrow();
        check(persisted.getStatus() == BuildStatus.QUEUED
                && messages(persisted.getLogs()).equals(List.of("Build queued")),
                "A build and its initial log must already be durable when it becomes schedulable");
        check(api.triggerBuild(secondary, "secondary build", true), "API should accept another site's build");
        check(queue.nextIfCapacityAvailable(1).isEmpty(), "The reservation must count toward capacity");

        queue.remove(reserved);
        new WebsiteBuilder(reserved, null) {}.run();
        check(reserved.getStatus() == BuildStatus.STOPPED, "A cancelled reservation must not start");
        WebsiteBuild running = queue.nextIfCapacityAvailable(1).orElseThrow();
        check(running.getWebsite().getId().equals(secondary.getId()), "Cancelled worker did not release its slot");
        check(running.beginBuild(), "The remaining build should be able to start");

        check(api.triggerBuild(primary, "final build"), "Expected a fresh build before shutdown");
        WebsiteBuild waiting = queue.getBuildQueue().get(0);
        check(queue.beginShutdown().containsAll(List.of(running, waiting)), "Shutdown must collect running and queued builds");
        check(running.getStatus() == BuildStatus.STOPPED && waiting.getStatus() == BuildStatus.STOPPED
                && queue.nextIfCapacityAvailable(2).isEmpty(), "Shutdown must stop active builds and disable scheduling");
        check(!api.triggerBuild(primary) && !api.triggerBuild(primary, false)
                && !api.triggerBuild(primary, "rejected") && !api.triggerBuild(primary, "rejected", false),
                "All API overloads must report rejected admission");

        return waiting;
    }

    private static void checkWebhookRejection(Website site) throws Exception {
        DeployHookRoute hook = new DeployHookRoute();
        // The queue is shutting down: a skip is still successful, whereas a real build is rejected.
        hook.post(deployRequest(site, "%5Bno+ci%5D"));
        expect(InternalServerErrorResponse.class, () -> hook.post(deployRequest(site, "build")));

        Predicate<Website> matches = candidate -> candidate.getId().equals(site.getId());
        expect(InternalServerErrorResponse.class,
                () -> GithubAppWebhookRoute.handlePush("{\"commits\":[{\"message\":\"build\"}]}", matches));
        expect(InternalServerErrorResponse.class,
                () -> GithubAppWebhookRoute.handleTag("{\"ref_type\":\"tag\",\"ref\":\"v1\"}", matches));
        expect(InternalServerErrorResponse.class, () -> GithubAppWebhookRoute.handleRelease(
                "{\"action\":\"released\",\"repository\":{\"full_name\":\"example/site\"},\"release\":{\"name\":\"v1\"}}",
                matches));
        GithubAppWebhookRoute.handlePush("{\"commits\":[{\"message\":\"[no ci]\"}]}", matches);
    }

    private static void checkWritesSurviveShutdown(BuildDatabase database, String primaryId, String secondaryId,
                                                  WebsiteBuild recovered, WebsiteBuild stopped) throws SQLException {
        List<WebsiteBuild> primary = database.getBuilds(primaryId, 10);
        check(primary.stream().map(WebsiteBuild::getCommit).toList()
                        .equals(List.of("commit 4 🚀", "commit 5 🚀", "commit 6 🚀",
                                "[no cache] <manual build>", "final build")),
                "Database pruning must keep exactly the newest five builds");
        check(database.getLogs(historicalId(3), 0).isEmpty(), "Pruning must also delete the discarded build's logs");
        check(database.getBuild(secondaryId, stopped.getId()).isEmpty(), "History must not expose builds of another site");
        check(database.getBuilds(secondaryId, 10).size() == 2, "Pruning one site must not remove another site's history");

        WebsiteBuild savedRecovery = database.getBuild(primaryId, recovered.getId()).orElseThrow();
        check(savedRecovery.getStatus() == BuildStatus.ERROR && savedRecovery.getEndTime() == recovered.getEndTime()
                && messages(savedRecovery.getLogs()).equals(List.of("first 🚀", "second", INTERRUPTED)),
                "Recovery status and diagnostic must both survive shutdown");
        List<SimofaLog> subsequentLogs = database.getLogs(recovered.getId(), 10);
        check(messages(subsequentLogs).equals(List.of(INTERRUPTED)), "Log reads must exclude the timestamp boundary");

        WebsiteBuild savedStop = database.getBuild(primaryId, stopped.getId()).orElseThrow();
        check(savedStop.getStatus() == BuildStatus.STOPPED && savedStop.getEndTime() == stopped.getEndTime()
                && messages(savedStop.getLogs()).equals(List.of("Build queued", "Build status changed to stopped")),
                "Shutdown must drain both pending status and log writes");
    }

    private static WebsiteBuild historicalBuild(Website site, int sequence, BuildStatus status) {
        List<SimofaLog> logs = List.of(
                new SimofaLog("INFO", "first 🚀", historicalId(101), 10),
                new SimofaLog("INFO", "second", historicalId(102), 10),
                new SimofaLog("INFO", "duplicate", historicalId(101), 11));
        return WebsiteBuild.fromDatabase(historicalId(sequence), site, "commit " + sequence + " 🚀",
                status, 10, 20, status == BuildStatus.DEPLOYED ? 30 : 0, logs, null);
    }

    private static String historicalId(int sequence) {
        return "00000000-0000-7000-8000-" + String.format(Locale.ROOT, "%012d", sequence);
    }

    private static Website website(String id, String name) {
        return new Website(id, name, "alpine", 128, 0.5, "https://example.test/site",
                "main", null, Website.BUILD_ON.COMMIT, "build", "deploy", "rollback", null, "test-token");
    }

    private static List<String> messages(List<SimofaLog> logs) {
        return logs.stream().map(SimofaLog::getLog).toList();
    }

    private static Context deployRequest(Website site, String commit) {
        Map<String, String> params = Map.of("website", site.getId(), "token", site.getDeployToken(), "commit", commit);
        return (Context) Proxy.newProxyInstance(Context.class.getClassLoader(), new Class<?>[]{Context.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("queryParam")) return params.get(args[0]);
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private static void requireEmptySchema(TestConfig config) throws SQLException {
        try (var connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/" + config.getDatabase(),
                config.getDbUsername(), config.getDbPassword());
             var statement = connection.createStatement();
             var tables = statement.executeQuery("SHOW TABLES")) {
            check(!tables.next(), "Use a disposable, empty schema for this test");
        }
    }

    private static void expect(Class<? extends Exception> type, CheckedAction action) throws Exception {
        try {
            action.run();
        } catch (Exception e) {
            if (type.isInstance(e)) return;
            throw e;
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }

    private static class TestConfig extends Config {
        private final String database = requiredEnv("SIMOFA_TEST_MYSQL_DATABASE");
        private final String username = requiredEnv("SIMOFA_TEST_MYSQL_USER");

        @Override
        public String getDatabase() {
            check(database.matches("[a-zA-Z0-9_]+"), "Use a simple MySQL test schema name");
            return database;
        }

        @Override
        public String getDbUsername() {
            return username;
        }

        @Override
        public String getDbPassword() {
            return System.getenv().getOrDefault("SIMOFA_TEST_MYSQL_PASSWORD", "");
        }

        @Override
        public String getS3AccessKeyId() {
            return "test-key";
        }

        @Override
        public String getS3SecretAccessKey() {
            return "test-secret";
        }

        private static String requiredEnv(String name) {
            String value = System.getenv(name);
            if (value == null || value.isBlank()) throw new IllegalArgumentException("Set " + name);
            return value;
        }
    }
}
