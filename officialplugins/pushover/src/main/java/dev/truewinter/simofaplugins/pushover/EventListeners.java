package dev.truewinter.simofaplugins.pushover;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.truewinter.PluginManager.EventHandler;
import dev.truewinter.PluginManager.Listener;
import dev.truewinter.PluginManager.Logger;
import dev.truewinter.simofa.api.events.BuildQueuedEvent;
import dev.truewinter.simofa.api.events.BuildStatusChangedEvent;

import java.util.List;

@SuppressWarnings("unused")
public class EventListeners implements Listener {
    private final PushoverPlugin plugin;
    private final YamlDocument config;
    private final Logger logger;

    protected EventListeners(PushoverPlugin plugin, YamlDocument config, Logger logger) {
        this.plugin = plugin;
        this.config = config;
        this.logger = logger;
    }

    @EventHandler
    public void onBuildStatusChange(BuildStatusChangedEvent e) {
        String buildStatus = e.getBuild().getStatus().toString();
        List<String> buildStatuses = config.getStringList("statuses");
        if (buildStatuses.contains(buildStatus)) {
            plugin.pushMessage(config.getString("title"), String.format(
                    "Build: %s%n" +
                    "Website: %s%n" +
                    "Status: %s",
                    e.getBuild().getId(),
                    e.getBuild().getWebsite().getId(),
                    buildStatus
            ));
        }
    }
}
