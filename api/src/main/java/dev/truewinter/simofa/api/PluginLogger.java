package dev.truewinter.simofa.api;

import dev.truewinter.PluginManager.Logger;

import java.util.Map;

public class PluginLogger extends Logger {
    private final org.slf4j.Logger logger;
    private static final Map<LogEvents, String> logStrings = Map.ofEntries(
            Map.entry(LogEvents.PLUGIN_LOADED, "Plugin %s loaded"),
            Map.entry(LogEvents.PLUGIN_UNLOADED, "Plugin %s unloaded"),
            Map.entry(LogEvents.PLUGIN_LOADING_ERROR, "Failed to load plugin %s"),
            Map.entry(LogEvents.PLUGIN_UNLOADING_ERROR, "Failed to unload plugin %s"),
            Map.entry(LogEvents.UNKNOWN_PLUGIN_ERROR, "Unknown plugin %s"),
            Map.entry(LogEvents.ALL_PLUGINS_LOADED_FAILED_CALL_ERROR, "Failed to call onAllPluginsLoaded() method for plugin %s"),
            Map.entry(LogEvents.EVENT_DISPATCH_CALL_ERROR, "Failed to dispatch event to plugin %s")
    );

    protected PluginLogger(String pluginName) {
        super(pluginName);
        logger = L4jLogger.getL4jLogger();
    }

    @Override
    public final void info(String s) {
        logger.info(String.format("[%s] %s", getPluginName(), s));
    }

    @Override
    public void warn(String s) {
        logger.warn(String.format("[%s] %s", getPluginName(), s));
    }

    @Override
    public void warn(String s, Throwable t) {
        logger.warn(String.format("[%s] %s", getPluginName(), s), t);
    }

    @Override
    public final void error(String s) {
        logger.error(String.format("[%s] %s", getPluginName(), s));
    }

    @Override
    public final void error(String s, Throwable t) {
        logger.error(String.format("[%s] %s", getPluginName(), s), t);
    }

    private static String getLogString(PluginManagerLog log) {
        String logString = logStrings.get(log.getEvent());

        if (logString == null) {
            logString = log.getEvent().name() + " log from plugin %s";
        }

        return logString;
    }

    /**
     * @hidden
     */
    protected static void handlePluginManagerLog(Logger.PluginManagerLog log) {
        String logString = String.format(getLogString(log), log.getPluginName());

        if (log.getException() != null) {
            L4jLogger.getL4jLogger().error(String.format(logString, log.getPluginName()), log.getException());
        } else  {
            L4jLogger.getL4jLogger().info(String.format(logString, log.getPluginName()));
        }
    }
}
