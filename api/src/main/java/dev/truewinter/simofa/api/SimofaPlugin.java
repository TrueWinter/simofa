package dev.truewinter.simofa.api;

import dev.truewinter.PluginManager.Listener;
import dev.truewinter.PluginManager.Logger;
import dev.truewinter.PluginManager.Plugin;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public abstract class SimofaPlugin extends Plugin<SimofaAPI> {
    @Override
    protected final void registerListeners(Plugin<SimofaAPI> plugin, Listener listener) throws Exception {
        ensureNoApiInteractionInConstructor();
        SimofaPluginManager.getInstance().getPluginManager().registerListener(plugin, listener);
    }

    @Override
    protected final Logger getLogger() {
        ensureNoApiInteractionInConstructor();
        return SimofaPluginManager.getInstance().getLogger(getName());
    }

    @Override
    protected final Plugin<SimofaAPI> getPluginByName(@NotNull String s) throws ClassCastException, IllegalStateException {
        ensureNoApiInteractionInConstructor();
        return SimofaPluginManager.getInstance().getPluginManager().getPluginByName(this, s);
    }

    @Override
    protected final SimofaAPI getApi() {
        ensureNoApiInteractionInConstructor();
        return SimofaPluginManager.getInstance().getApi();
    }
}
