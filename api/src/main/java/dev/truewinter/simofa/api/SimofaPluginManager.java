package dev.truewinter.simofa.api;

import dev.truewinter.PluginManager.Logger;
import dev.truewinter.PluginManager.PluginManager;

import java.util.HashMap;

/**
 * For <code>dev.truewinter.PluginManager</code> docs,
 * <a href="https://javadoc.jitpack.io/com/github/TrueWinter/PluginManager/latest/javadoc/">click here</a>.
 */
public class SimofaPluginManager {
    private static SimofaPluginManager simofaPluginManager;
    private final PluginManager<SimofaAPI> pluginManager;
    private final HashMap<String, Logger> logger = new HashMap<>();
    private final SimofaAPI api;

    private SimofaPluginManager(SimofaAPI api) {
        pluginManager = new PluginManager<>(getClass().getClassLoader(), PluginLogger::handlePluginManagerLog);
        this.api = api;
    }

    public static SimofaPluginManager getInstance(SimofaAPI api) {
        if (simofaPluginManager == null) {
            simofaPluginManager = new SimofaPluginManager(api);
        }

        return simofaPluginManager;
    }

    public static SimofaPluginManager getInstance() {
        if (simofaPluginManager == null) {
            throw new RuntimeException("PluginManager has not yet been initialized");
        }

        return simofaPluginManager;
    }

    public PluginManager<SimofaAPI> getPluginManager() {
        return pluginManager;
    }

    protected SimofaAPI getApi() {
        return api;
    }

    protected Logger getLogger(String pluginName) {
        if (!logger.containsKey(pluginName)) {
            logger.put(pluginName, new PluginLogger(pluginName));
        }

        return logger.get(pluginName);
    }
}
