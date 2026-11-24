package dev.truewinter.simofa.docker;

import dev.truewinter.simofa.Simofa;
import dev.truewinter.simofa.api.Website;
import dev.truewinter.simofa.api.WebsiteBuild;
import dev.truewinter.simofa.api.internal.WsRegistry;
import dev.truewinter.simofa.common.BuildStatus;
import dev.truewinter.simofa.common.LogType;
import dev.truewinter.simofa.common.SimofaLog;
import dev.truewinter.simofa.common.Util;
import dev.truewinter.simofa.config.Config;
import dev.truewinter.simofa.database.Database;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class BuildQueue {
    private static final long ADMISSION_SHUTDOWN_WAIT_SECONDS = 5;
    private static final Set<BuildStatus> STOPPABLE_STATUSES = Set.of(
            BuildStatus.QUEUED,
            BuildStatus.PREPARING,
            BuildStatus.BUILDING
    );

    private static BuildQueue buildQueue;
    private final Config config;
    private final Database database;
    private final ReentrantLock admissionLock = new ReentrantLock();
    // all builds
    private final HashMap<String, List<WebsiteBuild>> websiteBuildList = new HashMap<>();
    // builds that are queued
	// TODO: ArrayDeque?
	// add() and poll()
    private final List<WebsiteBuild> websiteBuildQueue = new ArrayList<>();
    // builds that are currently running
    private final List<WebsiteBuild> beingBuiltList = new ArrayList<>();
    private boolean stopping = false;

    private BuildQueue(Config config, Database database) {
        this.config = config;
        this.database = database;
    }

    protected static BuildQueue getInstance(Config config, Database database) {
        if (buildQueue == null) {
            buildQueue = new BuildQueue(config, database);
        }

        return buildQueue;
    }

    public boolean loadBuilds() {
        admissionLock.lock();

        try {
            return loadBuildsLocked();
        } finally {
            admissionLock.unlock();
        }
    }

    private synchronized boolean loadBuildsLocked() {
        try {
            Set<String> affectedWebsites = new HashSet<>();

            for (WebsiteBuild build : database.getBuildDatabase().getRecentBuilds(5)) {
                if (mergeLoadedBuild(build)) {
                    affectedWebsites.add(build.getWebsite().getId());
                }
            }

            for (String websiteId : affectedWebsites) {
                WsRegistry.accept(WsRegistry.Instances.BUILD_QUEUE, websiteId, null);
            }

            return true;
        } catch (SQLException e) {
            Simofa.getLogger().error("Failed to load builds from database", e);
            return false;
        }
    }

    private boolean mergeLoadedBuild(WebsiteBuild build) {
        List<WebsiteBuild> builds = websiteBuildList.computeIfAbsent(
                build.getWebsite().getId(), k -> new ArrayList<>());

        if (builds.stream().anyMatch(existing -> existing.getId().equals(build.getId()))) {
            return false;
        }

        attachPersistenceListener(build);

        if (!WebsiteBuild.END_STATUSES.contains(build.getStatus())) {
            build.restoreAsError("Build interrupted by server restart");
        }

        builds.add(build);
        builds.sort(Comparator.comparing(WebsiteBuild::getId));

        while (builds.size() > 5) {
            builds.remove(0);
        }

        return true;
    }

    private void attachPersistenceListener(WebsiteBuild build) {
        build.setPersistenceListener(new WebsiteBuild.PersistenceListener() {
            @Override
            public void onStatusChanged(WebsiteBuild build) {
                database.getBuildDatabase().queueBuildUpdate(build);
            }

            @Override
            public void onLogAdded(WebsiteBuild build, SimofaLog log) {
                database.getBuildDatabase().queueLog(build.getId(), log);
            }

            @Override
            public void onBuildRestoredAsError(WebsiteBuild build, SimofaLog log) {
                database.getBuildDatabase().queueBuildRecovery(build, log);
            }
        });
    }

    public synchronized Optional<WebsiteBuild> nextIfCapacityAvailable(int concurrentBuilds) {
        if (stopping || beingBuiltList.size() >= concurrentBuilds || websiteBuildQueue.isEmpty()) {
            return Optional.empty();
        }

        WebsiteBuild websiteBuild = websiteBuildQueue.remove(0);
        beingBuiltList.add(websiteBuild);
        return Optional.of(websiteBuild);
    }

    public synchronized void removeRunningBuild(WebsiteBuild build) {
        beingBuiltList.removeIf(existing -> existing.getId().equals(build.getId()));
    }

    public List<WebsiteBuild> beginShutdown() {
        List<WebsiteBuild> builds;

        synchronized (this) {
            stopping = true;

            builds = new ArrayList<>(beingBuiltList);
            builds.addAll(websiteBuildQueue);

            for (WebsiteBuild build : builds) {
                if (STOPPABLE_STATUSES.contains(build.getStatus())) {
                    build.setStatus(BuildStatus.STOPPED);
                }
            }

            beingBuiltList.clear();
            websiteBuildQueue.clear();
        }

        try {
            if (admissionLock.tryLock(ADMISSION_SHUTDOWN_WAIT_SECONDS, TimeUnit.SECONDS)) {
                admissionLock.unlock();
            } else {
                Simofa.getLogger().warn("Timed out waiting for in-flight build admissions during shutdown");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return builds;
    }

    public boolean queue(Website website, String commit) {
        if (commit.toLowerCase().contains("[no ci]")) {
            return true;
        }

        WebsiteBuild websiteBuild;
        String websiteId;

        admissionLock.lock();

        try {
            synchronized (this) {
                if (stopping) {
                    return false;
                }
            }

            websiteBuild = new WebsiteBuild(website, commit, config.getCacheDir());

            try {
                database.getBuildDatabase().addBuild(websiteBuild);
            } catch (SQLException e) {
                Simofa.getLogger().error("Failed to save build to database, rejecting build", e);
                return false;
            }

            websiteId = websiteBuild.getWebsite().getId();
            boolean admitted = false;

            synchronized (this) {
                if (!stopping) {
                    try {
                        remove(website);
                        admitted = true;
                    } catch (RuntimeException e) {
                        Simofa.getLogger().error("Failed to stop existing builds for website, rejecting build", e);
                    }

                    if (admitted) {
                        attachPersistenceListener(websiteBuild);
                        websiteBuildQueue.add(websiteBuild);

                        if (!websiteBuildList.containsKey(websiteId)) {
                            websiteBuildList.put(websiteId, new ArrayList<>());
                        }

                        List<WebsiteBuild> websiteBuildsForThisWebsite = websiteBuildList.get(websiteId);
                        websiteBuildsForThisWebsite.add(websiteBuild);
                        if (websiteBuildsForThisWebsite.size() > 5) {
                            websiteBuildsForThisWebsite.remove(0);
                        }

                        websiteBuild.announceQueued();
                    }
                }
            }

            if (!admitted) {
                try {
                    database.getBuildDatabase().deleteBuild(websiteBuild.getId());
                } catch (SQLException e) {
                    Simofa.getLogger().error("Failed to remove rejected build from database", e);
                }

                return false;
            }
        } finally {
            admissionLock.unlock();
        }

        pruneBuilds(websiteId);
        return true;
    }

    private void pruneBuilds(String websiteId) {
        try {
            database.getBuildDatabase().pruneBuilds(websiteId, 5);
        } catch (SQLException e) {
            Simofa.getLogger().error("Failed to prune old builds from database", e);
        }
    }

    public synchronized void remove(Website website) {
        remove(website.getId());
    }

    public synchronized void remove(Container container) {
        for (List<WebsiteBuild> builds : websiteBuildList.values()) {
             Optional<WebsiteBuild> build = builds.stream().filter(wb -> {
                 if (wb.getContainerId() != null) {
                     return wb.getContainerId().equals(container.getId());
                 }

                 return false;
             }).findFirst();
            if (build.isPresent()) {
                build.get().addLog(new SimofaLog(LogType.WARN, "Deletion was requested for the container used for this build"));
                remove(build.get().getWebsite());
                break;
            }
        }
    }

    // Settings may have changed since the build started,
    // meaning that the Website object may differ. However,
    // the ID will always stay the same for a website.
    public synchronized void remove(String id) {
        websiteBuildQueue.removeIf(w -> w.getWebsite().getId().equals(id));
        if (websiteBuildList.containsKey(id)) {
            websiteBuildList.get(id).forEach(this::remove);
        }

        beingBuiltList.removeIf(w -> w.getWebsite().getId().equals(id));
    }

    public synchronized void remove(WebsiteBuild w) {
        if (STOPPABLE_STATUSES.contains(w.getStatus())) {
            w.setStatus(BuildStatus.STOPPED);
        }

        if (!Util.isBlank(w.getContainerId())) {
            Simofa.getLogger().info(String.format("Deleting container %s for build %s", w.getContainerId(), w.getId()));
            Simofa.getDockerManager().deleteContainer(w.getContainerId());
        }
    }

    public synchronized List<WebsiteBuild> getCurrentBuilds() {
        return beingBuiltList;
    }

    public synchronized List<WebsiteBuild> getBuildQueue() {
        return websiteBuildQueue;
    }

    public synchronized HashMap<String, List<WebsiteBuild>> getWebsiteBuildList() {
        HashMap<String, List<WebsiteBuild>> snapshot = new HashMap<>();
        websiteBuildList.forEach((websiteId, builds) ->
                snapshot.put(websiteId, new ArrayList<>(builds)));
        return snapshot;
    }
}
