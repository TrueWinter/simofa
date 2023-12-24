package dev.truewinter.simofa.api.events;

import dev.truewinter.PluginManager.Event;
import dev.truewinter.simofa.api.WebsiteBuild;

/**
 * This event is dispatched when a build's status changes.
 * For queued builds, see {@link BuildQueuedEvent}
 */
public class BuildStatusChangedEvent extends Event {
    private final WebsiteBuild build;

    public BuildStatusChangedEvent(WebsiteBuild build) {
        this.build = build;
    }

    public WebsiteBuild getBuild() {
        return build;
    }
}
