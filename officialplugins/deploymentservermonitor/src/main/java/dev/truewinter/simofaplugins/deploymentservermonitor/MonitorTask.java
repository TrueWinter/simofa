package dev.truewinter.simofaplugins.deploymentservermonitor;

import dev.truewinter.PluginManager.Logger;
import dev.truewinter.PluginManager.Plugin;
import dev.truewinter.simofa.api.DeploymentServer;
import dev.truewinter.simofa.api.SimofaAPI;
import dev.truewinter.simofaplugins.pushover.PushoverPlugin;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TimerTask;

public class MonitorTask extends TimerTask {
    private final Logger logger;
    private final PushoverPlugin pushoverPlugin;
    private final SimofaAPI api;
    private final List<String> previouslyOfflineDeploymentServers = new ArrayList<>();

    protected MonitorTask(Plugin<SimofaAPI> pushoverPlugin, Logger logger, SimofaAPI api) {
        this.logger = logger;
        this.pushoverPlugin = (PushoverPlugin) pushoverPlugin;
        this.api = api;
    }

    private boolean isOnline(DeploymentServer deploymentServer, int attempt) throws IOException {
        if (attempt == 3) return false;

        URL url = new URL(deploymentServer.getUrl() +
                (deploymentServer.getUrl().endsWith("/") ? "" : "/") + "status");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setConnectTimeout(5000);
        con.setReadTimeout(5000);
        int status = con.getResponseCode();
        if (status != 200) {
            con.disconnect();
            return isOnline(deploymentServer, attempt + 1);
        }

        con.disconnect();
        return true;
    }

    @Override
    public void run() {
        try {
            List<DeploymentServer> deploymentServers = api.getDeploymentServers();
            HashMap<DeploymentServer, String> offlineDeploymentServers = new HashMap<>();

            for (DeploymentServer deploymentServer : deploymentServers) {
                try {
                    if (!isOnline(deploymentServer, 0)) {
                        throw new Exception("Non-200 status");
                    } else {
                        previouslyOfflineDeploymentServers.remove(deploymentServer.getId());
                    }
                } catch (Exception e) {
                    if (!previouslyOfflineDeploymentServers.contains(deploymentServer.getId())) {
                        offlineDeploymentServers.put(deploymentServer, e.getMessage());
                        previouslyOfflineDeploymentServers.add(deploymentServer.getId());
                    }
                }
            }

            if (!offlineDeploymentServers.isEmpty()) {
                StringBuilder stringBuilder = new StringBuilder();
                offlineDeploymentServers.forEach((d, m) -> stringBuilder.append(String.format("- %s: %s%n", d.getName(), m)));
                pushoverPlugin.pushMessage("Deployment Server(s) Offline", stringBuilder.toString());
            }
        } catch (Exception e) {
            logger.error("Failed to check deployment server status", e);
        }
    }
}
