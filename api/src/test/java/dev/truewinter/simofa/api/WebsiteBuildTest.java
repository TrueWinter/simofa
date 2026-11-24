package dev.truewinter.simofa.api;

import dev.truewinter.simofa.api.internal.WsRegistry;
import dev.truewinter.simofa.common.BuildStatus;
import dev.truewinter.simofa.common.LogType;
import dev.truewinter.simofa.common.SimofaLog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// Run main in a fresh JVM with the API module's test runtime classpath.
public class WebsiteBuildTest {
    private static final Website WEBSITE = new Website("site-id", "Test site", "alpine", 128, 0.5,
            "https://example.test/site", "main", null, Website.BUILD_ON.COMMIT,
            "build", "deploy", "rollback", null, "test-token");

    public static void main(String[] args) throws Exception {
        SimofaPluginManager.getInstance(null);
        restoresStoredFieldsAndOwnsItsLogList();
        recoversEveryInterruptedStatusExactlyOnce();
        leavesCompletedBuildsUnchanged();
        admitsOnlyOneConcurrentStart();
        publishesCommittedStateAndHonorsCancellation();
        announcesNewBuildsExplicitly();
    }

    private static void restoresStoredFieldsAndOwnsItsLogList() {
        SimofaLog first = new SimofaLog("INFO", "first", "first-log", 101);
        SimofaLog second = new SimofaLog("WARN", "second", "second-log", 102);
        List<SimofaLog> storedLogs = new ArrayList<>(List.of(first, second));
        WebsiteBuild build = storedBuild(BuildStatus.DEPLOYED, 300, storedLogs);
        storedLogs.clear();

        check(build.getId().equals("stored-id") && build.getWebsite() == WEBSITE
                && build.getCommit().equals("stored commit"), "Restoration lost build identity or commit");
        check(build.getStatus() == BuildStatus.DEPLOYED && build.getQueuedAt() == 100
                && build.getStartTime() == 200 && build.getEndTime() == 300 && build.getRunTime() == 100,
                "Restoration lost lifecycle fields");
        check(build.getLogs().equals(List.of(first, second)), "Restoration must retain the original log order");
        check(build.getContainerId() == null, "A restored build must not own a live container");
        check(new File("cache", "website-site-id").equals(build.getCacheDir()), "Cache path was not restored");
    }

    private static void recoversEveryInterruptedStatusExactlyOnce() {
        for (BuildStatus status : List.of(BuildStatus.QUEUED, BuildStatus.PREPARING,
                BuildStatus.BUILDING, BuildStatus.DEPLOYING)) {
            WebsiteBuild build = storedBuild(status, 0, null);
            long originalStart = build.getStartTime();
            RecordingListener listener = new RecordingListener();
            build.setPersistenceListener(listener);
            check(build.getLogs().isEmpty(), "Missing stored logs should be treated as empty");

            build.restoreAsError("interrupted");
            long finishedAt = build.getEndTime();
            build.restoreAsError("duplicate");

            check(build.getStatus() == BuildStatus.ERROR && build.getStartTime() == originalStart
                    && finishedAt > 200 && build.getEndTime() == finishedAt, "Incorrect recovery for " + status);
            check(build.getLogs().size() == 1 && listener.recoveries.equals(build.getLogs()),
                    "Recovery must record one log through the combined persistence callback");
            SimofaLog log = build.getLogs().get(0);
            check(log.getType().equals(LogType.ERROR.toString()) && log.getLog().equals("interrupted"),
                    "Incorrect recovery diagnostic");
            check(listener.logs.isEmpty() && listener.statuses.isEmpty(),
                    "Recovery must not also persist its status and log separately");
        }
    }

    private static void leavesCompletedBuildsUnchanged() {
        for (BuildStatus status : List.of(BuildStatus.DEPLOYED, BuildStatus.ERROR, BuildStatus.STOPPED)) {
            SimofaLog log = new SimofaLog(LogType.INFO, "existing log");
            WebsiteBuild build = storedBuild(status, 300, List.of(log));
            RecordingListener listener = new RecordingListener();
            build.setPersistenceListener(listener);

            build.restoreAsError("should not appear");
            check(!build.beginBuild(), "A terminal build cannot start: " + status);
            check(build.getStatus() == status && build.getEndTime() == 300 && build.getLogs().equals(List.of(log)),
                    "Recovery mutated a terminal build: " + status);
            check(listener.recoveries.isEmpty() && listener.statuses.isEmpty() && listener.logs.isEmpty(),
                    "Terminal builds should not be written again");
        }
    }

    private static void admitsOnlyOneConcurrentStart() throws Exception {
        WebsiteBuild build = storedBuild(BuildStatus.QUEUED, 0, List.of());
        RecordingListener listener = new RecordingListener();
        build.setPersistenceListener(listener);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        var workers = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> attempt = () -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) throw new AssertionError("Start signal timed out");
                return build.beginBuild();
            };
            var first = workers.submit(attempt);
            var second = workers.submit(attempt);
            check(ready.await(5, TimeUnit.SECONDS), "Workers did not reach the start gate");
            start.countDown();
            check(first.get(5, TimeUnit.SECONDS) ^ second.get(5, TimeUnit.SECONDS), "Only one worker may start");
        } finally {
            workers.shutdownNow();
        }

        check(build.getStatus() == BuildStatus.PREPARING && listener.statuses.equals(List.of(BuildStatus.PREPARING)),
                "The successful start should publish precisely one PREPARING transition");
        check(listener.logs.size() == 1 && listener.logs.equals(build.getLogs()),
                "Only the successful start should publish a status log");
    }

    private static void publishesCommittedStateAndHonorsCancellation() {
        WebsiteBuild build = storedBuild(BuildStatus.PREPARING, 0, List.of());
        RecordingListener listener = new RecordingListener();
        build.setPersistenceListener(listener);

        build.setStatus(BuildStatus.BUILDING);
        build.setStatus(BuildStatus.STOPPED);
        long endedAt = build.getEndTime();
        build.setStatus(BuildStatus.DEPLOYED);

        check(build.getStatus() == BuildStatus.STOPPED && build.getStartTime() > 0 && build.getEndTime() == endedAt
                && endedAt >= build.getStartTime(), "Late completion must not overwrite a cancelled build");
        check(listener.statuses.equals(List.of(BuildStatus.BUILDING, BuildStatus.STOPPED)),
                "Persistence must see the new status, without publishing the rejected transition");
        check(listener.logs.size() == 2 && listener.logs.equals(build.getLogs()),
                "Only accepted transitions should persist status logs");

        build.setPersistenceListener(null);
        build.addLog(new SimofaLog(LogType.INFO, "local log"));
        check(build.getLogs().size() == 3 && listener.logs.size() == 2,
                "Detaching persistence must still allow local logs");
    }

    private static void announcesNewBuildsExplicitly() {
        List<Object> queueAnnouncements = new ArrayList<>();
        WsRegistry.registerWsConsumer(WsRegistry.Instances.BUILD_QUEUE, (audience, data) -> queueAnnouncements.add(audience));

        WebsiteBuild build = new WebsiteBuild(WEBSITE, "new commit", null);
        check(queueAnnouncements.isEmpty(), "Constructing an unadmitted build must not publish a queue update");
        check(build.getStatus() == BuildStatus.QUEUED && build.getLogs().size() == 1
                && build.getLogs().get(0).getLog().equals("Build queued"), "Missing initial state or queue log");
        build.announceQueued();
        check(queueAnnouncements.equals(List.of(WEBSITE.getId())), "Queue update must reach the build's website");
    }

    private static WebsiteBuild storedBuild(BuildStatus status, long endTime, List<SimofaLog> logs) {
        long startTime = status == BuildStatus.QUEUED || status == BuildStatus.PREPARING ? 0 : 200;
        return WebsiteBuild.fromDatabase("stored-id", WEBSITE, "stored commit", status,
                100, startTime, endTime, logs, "cache");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static class RecordingListener implements WebsiteBuild.PersistenceListener {
        final List<BuildStatus> statuses = new ArrayList<>();
        final List<SimofaLog> logs = new ArrayList<>();
        final List<SimofaLog> recoveries = new ArrayList<>();

        @Override
        public void onStatusChanged(WebsiteBuild build) {
            statuses.add(build.getStatus());
        }

        @Override
        public void onLogAdded(WebsiteBuild build, SimofaLog log) {
            logs.add(log);
        }

        @Override
        public void onBuildRestoredAsError(WebsiteBuild build, SimofaLog log) {
            recoveries.add(log);
        }
    }
}
