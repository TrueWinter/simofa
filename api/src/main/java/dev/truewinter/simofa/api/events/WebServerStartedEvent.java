package dev.truewinter.simofa.api.events;

import dev.truewinter.PluginManager.Event;

/**
 * This event is dispatched when the web server has been started.
 * Any API methods related to the web server must wait until this
 * event has been dispatched.
 */
public class WebServerStartedEvent extends Event {
}
