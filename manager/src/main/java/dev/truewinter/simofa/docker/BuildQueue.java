package dev.truewinter.simofa.docker;

import dev.truewinter.simofa.Simofa;
import dev.truewinter.simofa.api.Website;
import dev.truewinter.simofa.api.WebsiteBuild;
import dev.truewinter.simofa.common.BuildStatus;
import dev.truewinter.simofa.common.LogType;
import dev.truewinter.simofa.common.SimofaLog;
import dev.truewinter.simofa.common.Util;
import dev.truewinter.simofa.config.Config;
import dev.truewinter.simofa.database.Database;

import java.sql.SQLException;
import java.util.*;

public class BuildQueue {
    private static BuildQueue buildQueue;
    private final Config config;
    private final Database database;
    // all builds
    private final HashMap<String, List<WebsiteBuild>> websiteBuildList = new HashMap<>();
    // builds that are queued
	// TODO: ArrayDeque?
	// add() and poll()
    private final List<WebsiteBuild> websiteBuildQueue = new ArrayList<>();
    // builds that are currently running
    private final List<WebsiteBuild> beingBuiltList = new ArrayList<>();

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

    public synchronized void loadBuilds() {
        websiteBuildList.clear();

        try {
            for (WebsiteBuild build : database.getBuildDatabase().getRecentBuilds(5)) {
                attachPersistenceListener(build);

                if (!WebsiteBuild.END_STATUSES.contains(build.getStatus())) {
                    build.restoreAsError("Build interrupted by server restart");
                }

                websiteBuildList.computeIfAbsent(build.getWebsite().getId(), k -> new ArrayList<>())
                        .add(build);
            }
        } catch (SQLException e) {
            Simofa.getLogger().error("Failed to load builds from database", e);
        }
    }

    private void attachPersistenceListener(WebsiteBuild build) {
        build.setPersistenceListener(new WebsiteBuild.PersistenceListener() {
            @Override
            public void onStatusChanged(WebsiteBuild build) {
                updateBuild(build);
            }

            @Override
            public void onLogAdded(WebsiteBuild build, SimofaLog log) {
                database.getBuildDatabase().queueLog(build.getId(), log);
            }
        });
    }

    private void updateBuild(WebsiteBuild build) {
        try {
            database.getBuildDatabase().updateBuild(build);
        } catch (SQLException e) {
            Simofa.getLogger().error("Failed to update build in database", e);
        }
    }

    public boolean canRunMoreConcurrentBuilds() {
        int concurrentBuilds = config.getConcurrentBuilds();

        synchronized (this) {
            return beingBuiltList.size() < concurrentBuilds;
        }
    }

    public synchronized void queue(Website website, String commit) {
        if (commit.toLowerCase().contains("[no ci]")) return;

        remove(website);
        WebsiteBuild websiteBuild = new WebsiteBuild(website, commit, config.getCacheDir());

        boolean persisted = false;
        try {
            database.getBuildDatabase().addBuild(websiteBuild);
            persisted = true;
        } catch (SQLException e) {
            Simofa.getLogger().error("Failed to save build to database", e);
        }

        if (persisted) {
            attachPersistenceListener(websiteBuild);
        }

        websiteBuildQueue.add(websiteBuild);

        String websiteId = websiteBuild.getWebsite().getId();
        if (!websiteBuildList.containsKey(websiteId)) {
            websiteBuildList.put(websiteId, new ArrayList<>());
        }

        List<WebsiteBuild> websiteBuildsForThisWebsite = websiteBuildList.get(websiteId);
        websiteBuildsForThisWebsite.add(websiteBuild);
        if (websiteBuildsForThisWebsite.size() > 5) {
            websiteBuildsForThisWebsite.remove(0);
        }

        if (database.getBuildDatabase().flushPendingLogs()) {
            try {
                database.getBuildDatabase().pruneBuilds(websiteId, 5);
            } catch (SQLException e) {
                Simofa.getLogger().error("Failed to prune old builds from database", e);
            }
        } else {
            Simofa.getLogger().warn("Skipping pruning old builds because pending build logs could not be saved");
        }
    }

    public synchronized Optional<WebsiteBuild> next() {
        if (websiteBuildQueue.isEmpty()) {
            return Optional.empty();
        }

        WebsiteBuild websiteBuild = websiteBuildQueue.get(0);
        websiteBuildQueue.remove(0);
        beingBuiltList.add(websiteBuild);
        return Optional.of(websiteBuild);
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
        if (!w.getStatus().equals(BuildStatus.STOPPED)) {
            Set<BuildStatus> statusSet = new HashSet<>(){{
                add(BuildStatus.QUEUED);
                add(BuildStatus.PREPARING);
                add(BuildStatus.BUILDING);
            }};
            if (statusSet.contains(w.getStatus())) {
                w.setStatus(BuildStatus.STOPPED);
            }

            if (!Util.isBlank(w.getContainerId())) {
                Simofa.getLogger().info(String.format("Deleting container %s for build %s", w.getContainerId(), w.getId()));
                Simofa.getDockerManager().deleteContainer(w.getContainerId());
            }
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
