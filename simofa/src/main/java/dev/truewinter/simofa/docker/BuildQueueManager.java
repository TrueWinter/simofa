package dev.truewinter.simofa.docker;

import dev.truewinter.simofa.Simofa;
import dev.truewinter.simofa.common.BuildStatus;
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

                for (WebsiteBuild w : new ArrayList<>(buildQueue.getCurrentBuilds())) {
                    if (w.getStatus().equals(BuildStatus.BUILDING.toString()) && w.getContainerId() != null) {
                        long TWENTY_MINUTES = 20 * 60 * 1000;
                        if (w.getRunTime() > TWENTY_MINUTES) {
                            buildQueue.remove(w.getWebsite());
                            System.out.printf("%s [%s]: %s %d%n", w.getId(), w.getContainerId(), w.getStatus(), w.getRunTime());
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
