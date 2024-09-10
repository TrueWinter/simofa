package dev.truewinter.simofa.docker;

import dev.truewinter.simofa.Simofa;
import dev.truewinter.simofa.api.WebsiteBuild;
import dev.truewinter.simofa.common.BuildStatus;
import dev.truewinter.simofa.common.LogType;
import dev.truewinter.simofa.common.SimofaLog;
import dev.truewinter.simofa.config.Config;
import dev.truewinter.simofa.database.Database;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class BuildQueueManager {
    private static BuildQueueManager buildQueueManager;
    private final BuildQueue buildQueue;
    private static Config config;
    private static Database database;
    private Timer timer;

    private BuildQueueManager(Config config) {
        this.buildQueue = BuildQueue.getInstance(config);
    }

    public static BuildQueueManager getInstance(Config config, Database database) {
        if (buildQueueManager == null) {
            buildQueueManager = new BuildQueueManager(config);
            BuildQueueManager.config = config;
            BuildQueueManager.database = database;
        }

        return buildQueueManager;
    }

    public BuildQueue getBuildQueue() {
        return buildQueue;
    }

    protected static Config getConfig() {
        return config;
    }

    protected static Database getDatabase() {
        return database;
    }

    public void start() {
        if (timer != null) return;

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (buildQueue.canRunMoreConcurrentBuilds()) {
                    buildQueue.next().ifPresent(w -> {
                        new WebsiteBuilder(w).start();
                    });
                }

                long TWENTY_MINUTES = 20 * 60 * 1000;

                // Loop through a snapshot of the list
                for (WebsiteBuild w : new ArrayList<>(buildQueue.getCurrentBuilds())) {
                    if (w.getStatus().equals(BuildStatus.BUILDING) && w.getContainerId() != null) {
                        if (w.getRunTime() > TWENTY_MINUTES) {
                            buildQueue.remove(w.getWebsite());
                            w.addLog(new SimofaLog(LogType.ERROR, "Build duration has exceeded 20 minutes"));
                        }
                    }
                }

                for (WebsiteBuild w : new ArrayList<>(buildQueue.getBuildQueue())) {
                    if (w.getStatus().equals(BuildStatus.QUEUED)) {
                        if (System.currentTimeMillis() - w.getQueuedAt() > TWENTY_MINUTES) {
                            w.setStatus(BuildStatus.ERROR);
                            w.addLog(new SimofaLog(LogType.ERROR, "Build has been queued for longer than 20 minutes"));
                            buildQueue.remove(w.getWebsite());
                        }
                    }
                }
            }
        }, 0, 1000);
    }

    public void stop() {
        if (timer != null) {
            timer.cancel();
        }

        Simofa.getLogger().info("Removing builds from queue");

        buildQueue.getBuildQueue().forEach(w -> {
            buildQueue.remove(w.getWebsite());
        });
    }
}
