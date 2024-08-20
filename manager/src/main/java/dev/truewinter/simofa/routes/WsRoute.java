package dev.truewinter.simofa.routes;

import dev.truewinter.simofa.WsManager;
import io.javalin.websocket.WsContext;
import io.javalin.websocket.WsMessageContext;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public abstract class WsRoute {
    protected final WsContext ctx;

    public WsRoute(WsContext ctx) {
        this.ctx = ctx;
    }

    public abstract void handle();
    public abstract String getRoom();
    public void handleMessage(WsMessageContext ctx) {}
    public void closeInactiveConnections() {}

    public void sendEvent(String type, Object data) {
        sendEvent(ctx, type, data);
    }

    public static void sendEvent(WsContext ctx, String type, Object data) {
        HashMap<String, Object> event = new HashMap<>();
        event.put("type", type);
        event.put("data", data);
        ctx.send(event);
    }

    public void ping() {
        try {
            ctx.sendPing();
        } catch (Exception ignored) {}
    }

    @Nullable
    protected ConcurrentHashMap<String, WsRoute> getClients() {
        return getClients(getRoom());
    }

    @Nullable
    protected static ConcurrentHashMap<String, WsRoute> getClients(String room) {
        return WsManager.getClients(room);
    }
}
