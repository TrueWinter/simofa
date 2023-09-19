package dev.truewinter.simofadeploy;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;

public class DeployQueue {
    private final ArrayDeque<WebsiteDeployment> queue = new ArrayDeque<>();
    // Possibly support multiple simultaneous deployments
    // for different websites in the future
    //private final ArrayList<WebsiteDeployment> deployingList = new ArrayList<>();
    private final ArrayList<WebsiteDeployer> deployerList = new ArrayList<>();
    private static DeployQueue deployQueue;

    private DeployQueue() {}

    public static DeployQueue getInstance() {
        if (deployQueue == null) {
            deployQueue = new DeployQueue();
        }

        return deployQueue;
    }

    public synchronized void queue(WebsiteDeployment websiteDeployment) {
        SimofaDeploy.getLogger().info(String.format("Build %s queued for deployment", websiteDeployment.getBuildId()));
        queue.add(websiteDeployment);
    }

    public synchronized void done(WebsiteDeployment websiteDeployment) {
        //deployingList.removeIf(w -> w.equals(websiteDeployment));
        deployerList.removeIf(w -> w.getWebsiteDeployment().getBuildId().equals(websiteDeployment.getBuildId()));
    }

    @Nullable
    public synchronized WebsiteDeployment next() {
        WebsiteDeployment websiteDeployment = queue.poll();
        if (websiteDeployment == null) {
            return null;
        }

        SimofaDeploy.getLogger().info(String.format("Deploying build %s", websiteDeployment.getBuildId()));
        //deployingList.add(websiteDeployment);
        //deployerList.add(websiteDeployment);
        return websiteDeployment;
    }

    public synchronized void addWebsiteDeployer(WebsiteDeployer websiteDeployer) {
        deployerList.add(websiteDeployer);
    }

    public synchronized ArrayList<WebsiteDeployer> getDeployerList() {
        return deployerList;
    }
}
