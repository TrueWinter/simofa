package dev.truewinter.simofa.deploy;

import java.util.Timer;
import java.util.TimerTask;

public class DeployQueueManager {
    private static DeployQueueManager deployQueueManager;
    private DeployQueue deployQueue;
    private Timer timer;

    private DeployQueueManager() {
        deployQueue = DeployQueue.getInstance();

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (deployQueue.getDeployerList().size() == 0) {
                    WebsiteDeployment websiteDeployment = deployQueue.next();
                    if (websiteDeployment != null) {
                        WebsiteDeployer websiteDeployer = new WebsiteDeployer(websiteDeployment);
                        deployQueue.addWebsiteDeployer(websiteDeployer);
                        websiteDeployer.start();
                    }
                }
            }
        }, 0, 1000);
    }

    public static DeployQueueManager getInstance() {
        if (deployQueueManager == null) {
            deployQueueManager = new DeployQueueManager();
        }

        return deployQueueManager;
    }

    public DeployQueue getDeployQueue() {
        return deployQueue;
    }

    public void stop() {
        if (timer != null) {
            timer.cancel();
        }

        // See comment in ScriptExecutor
        /*for (WebsiteDeployer websiteDeployer : deployQueue.getDeployerList()) {
            websiteDeployer.stopAndRollBack();
        }*/
    }
}
