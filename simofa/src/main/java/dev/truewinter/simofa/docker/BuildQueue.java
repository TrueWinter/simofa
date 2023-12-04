package dev.truewinter.simofa.docker;

import dev.truewinter.simofa.Simofa;
import dev.truewinter.simofa.Website;
import dev.truewinter.simofa.common.BuildStatus;
import dev.truewinter.simofa.common.LogType;
import dev.truewinter.simofa.common.SimofaLog;
import dev.truewinter.simofa.common.Util;
import dev.truewinter.simofa.config.Config;

import java.util.*;

public class BuildQueue {
    private static BuildQueue buildQueue;
    private final Config config;
    // all builds
    private final HashMap<Integer, List<WebsiteBuild>> websiteBuildList = new HashMap<>();
    // builds that are queued
	// TODO: ArrayDeque?
	// add() and poll()
    private final List<WebsiteBuild> websiteBuildQueue = new ArrayList<>();
    // builds that are currently running
    private final List<WebsiteBuild> beingBuiltList = new ArrayList<>();

    private BuildQueue(Config config) {
        this.config = config;
    }

    protected static BuildQueue getInstance(Config config) {
        if (buildQueue == null) {
            buildQueue = new BuildQueue(config);
        }

        return buildQueue;
    }

    public boolean canRunMoreConcurrentBuilds() {
        int concurrentBuilds = config.getConcurrentBuilds();

        synchronized (this) {
            return beingBuiltList.size() < concurrentBuilds;
        }
    }

    public synchronized void queue(Website website, String commit) {
        remove(website);
        WebsiteBuild websiteBuild = new WebsiteBuild(website, commit, config.getCacheDir());
        websiteBuildQueue.add(websiteBuild);

        if (!websiteBuildList.containsKey(websiteBuild.getWebsite().getId())) {
            websiteBuildList.put(websiteBuild.getWebsite().getId(), new ArrayList<>());
        }

        List<WebsiteBuild> websiteBuildsForThisWebsite = websiteBuildList.get(websiteBuild.getWebsite().getId());
        websiteBuildsForThisWebsite.add(websiteBuild);
        if (websiteBuildsForThisWebsite.size() > 5) {
            websiteBuildsForThisWebsite.remove(0);
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
    public synchronized void remove(int id) {
        websiteBuildQueue.removeIf(w -> w.getWebsite().getId() == id);
        if (websiteBuildList.containsKey(id)) {
            websiteBuildList.get(id).forEach(w -> {
                if (w.getStatus().equals(BuildStatus.QUEUED.toString()) ||
                        w.getStatus().equals(BuildStatus.BUILDING.toString())) {
                    w.setStatus(BuildStatus.STOPPED);

                    System.out.printf("Stopping build %s%n", w.getId());
                    if (!Util.isBlank(w.getContainerId())) {
                        System.out.printf("Build %s has container %s%n", w.getId(), w.getContainerId());
                        Simofa.getDockerManager().deleteContainer(w.getContainerId());
                    }
                }
            });
        }

        beingBuiltList.removeIf(w -> w.getWebsite().getId() == id);
    }

    public synchronized List<WebsiteBuild> getCurrentBuilds() {
        return beingBuiltList;
    }

    public synchronized List<WebsiteBuild> getBuildQueue() {
        return websiteBuildQueue;
    }

    public HashMap<Integer, List<WebsiteBuild>> getWebsiteBuildList() {
        return websiteBuildList;
    }
}
