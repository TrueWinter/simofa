package dev.truewinter.simofa.api.events;

import dev.truewinter.PluginManager.Event;
import dev.truewinter.simofa.api.WebsiteBuild;

/**
 * This event is dispatched when a build is queued
 */
public class BuildQueuedEvent extends Event {
    private final WebsiteBuild build;

    public BuildQueuedEvent(WebsiteBuild build) {
        this.build = build;
    }

    public WebsiteBuild getBuild() {
        return build;
    }
}
