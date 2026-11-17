package dev.truewinter.simofa.docker;

import dev.truewinter.simofa.Simofa;
import dev.truewinter.simofa.api.WebsiteBuild;
import dev.truewinter.simofa.common.BuildStatus;
import dev.truewinter.simofa.common.LogType;
import dev.truewinter.simofa.common.SimofaLog;
import dev.truewinter.simofa.config.Config;
import dev.truewinter.simofa.database.Database;
import dev.truewinter.simofa.s3.S3CleanupTask;
import dev.truewinter.simofa.s3.S3Service;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class BuildQueueManager {
    private static BuildQueueManager buildQueueManager;
    private final BuildQueue buildQueue;
    private static Config config;
    private static Database database;
    private Timer buildQueueTimer;
    private Timer cleanupTimer;
    private static S3Service s3Service;
    private static S3CleanupTask s3CleanupTask;

    private BuildQueueManager(Config config, Database database) {
        this.buildQueue = BuildQueue.getInstance(config, database);
    }

    public static BuildQueueManager getInstance(Config config, Database database) {
        if (buildQueueManager == null) {
            buildQueueManager = new BuildQueueManager(config, database);
            BuildQueueManager.config = config;
            BuildQueueManager.database = database;
            BuildQueueManager.s3Service = new S3Service(config);
            BuildQueueManager.s3CleanupTask = new S3CleanupTask(s3Service);
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

    public void loadBuilds() {
        buildQueue.loadBuilds();
    }

    public void start() {
        if (buildQueueTimer == null) {
            buildQueueTimer = new Timer();
            buildQueueTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (buildQueue.canRunMoreConcurrentBuilds()) {
                        buildQueue.next().ifPresent(w -> {
                            new WebsiteBuilder(w, s3Service).start();
                        });
                    }

                    long FIVE_MINUTES = 5 * 60 * 1000;
                    long TWENTY_MINUTES = 20 * 60 * 1000;

                    // Loop through a snapshot of the list
                    for (WebsiteBuild w : new ArrayList<>(buildQueue.getCurrentBuilds())) {
                        if (w.getStatus().equals(BuildStatus.BUILDING) && w.getContainerId() != null) {
                            if (w.getRunTime() > TWENTY_MINUTES) {
                                buildQueue.remove(w.getWebsite());
                                w.addLog(new SimofaLog(LogType.ERROR, "Build duration has exceeded 20 minutes"));
                                w.setStatus(BuildStatus.ERROR);
                            }
                        }

                        if (w.getStatus().equals(BuildStatus.DEPLOYING) && w.getContainerId() != null) {
                            if (w.getRunTime() > FIVE_MINUTES) {
                                buildQueue.remove(w.getWebsite());
                                w.addLog(new SimofaLog(LogType.ERROR, "Deploy duration has exceeded 5 minutes"));
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

        if (cleanupTimer == null) {
            cleanupTimer = new Timer();
            cleanupTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    s3CleanupTask.run();
                }
            }, 0, 60 * 1000);
        }
    }

    public void stop() {
        if (buildQueueTimer != null) {
            buildQueueTimer.cancel();
        }

        if (cleanupTimer != null) {
            cleanupTimer.cancel();
        }

        Simofa.getLogger().info("Stopping builds");

        for (WebsiteBuild w : new ArrayList<>(buildQueue.getCurrentBuilds())) {
            buildQueue.remove(w);
        }

        for (WebsiteBuild w : new ArrayList<>(buildQueue.getBuildQueue())) {
            buildQueue.remove(w.getWebsite());
        }
    }
}
