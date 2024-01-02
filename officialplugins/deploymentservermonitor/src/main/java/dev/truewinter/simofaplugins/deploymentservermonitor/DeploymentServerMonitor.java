package dev.truewinter.simofaplugins.deploymentservermonitor;

import dev.truewinter.PluginManager.Plugin;
import dev.truewinter.PluginManager.UsesAnotherPlugin;
import dev.truewinter.simofa.api.SimofaAPI;
import dev.truewinter.simofa.api.SimofaPlugin;

import java.util.Timer;

@SuppressWarnings("unused")
public class DeploymentServerMonitor extends SimofaPlugin implements UsesAnotherPlugin {
    private Timer monitorTimer = null;

    @Override
    protected void onLoad() {}

    @Override
    protected void onUnload() {
        if (monitorTimer != null) {
            monitorTimer.cancel();
        }
    }

    @Override
    public void onAllPluginsLoaded() {
        // The PushoverPlugin class is intentionally not imported in this file
        // so that the error below can be shown if the plugin isn't installed.
        Plugin<SimofaAPI> pushoverPlugin = getPluginByName("Pushover");
        if (pushoverPlugin == null) {
            getLogger().error("Pushover plugin must be installed");
            return;
        }

        MonitorTask monitorTask = new MonitorTask(pushoverPlugin, getLogger(), getApi());

        monitorTimer = new Timer();
        monitorTimer.scheduleAtFixedRate(monitorTask, 10 * 1000, 5 * 60 * 1000);
    }
}
