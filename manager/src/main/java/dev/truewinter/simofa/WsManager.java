package dev.truewinter.simofa;

import dev.truewinter.simofa.common.Util;

import dev.truewinter.simofa.routes.WsRoute;
import io.javalin.websocket.WsConfig;
import io.javalin.websocket.WsContext;
import io.javalin.websocket.WsMessageContext;
import org.eclipse.jetty.websocket.core.CloseStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public abstract class WsManager {
    protected static final ConcurrentHashMap<String, ConcurrentHashMap<String, WsRoute>> clients = new ConcurrentHashMap<>();

    public static void init(WsConfig ws, Class<? extends WsRoute> handler) {
        ws.onConnect(ctx -> onConnect(ctx, newInstance(handler, ctx)));
        ws.onMessage(ctx -> onMessage(ctx, newInstance(handler, ctx)));
        ws.onClose(ctx -> onClose(ctx, newInstance(handler, ctx)));

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                clients.forEach((s, q) -> q.forEach((i, c) -> {
                    c.ping();
                    c.closeInactiveConnections();
                }));
            }
        }, 0, 30 * 1000);
    }

    @Nullable
    public static ConcurrentHashMap<String, WsRoute> getClients(String room) {
        return clients.get(room);
    }

    private static WsRoute newInstance(Class<? extends WsRoute> handler, WsContext ctx) throws Exception {
        return handler.getDeclaredConstructor(WsContext.class).newInstance(ctx);
    }

    private static void onConnect(WsContext ctx, WsRoute handler) {
        String token = ctx.queryParam("token");

        String room = handler.getRoom();
        WsToken wsToken = WsToken.jwtToWsToken(token);
        if (Util.isBlank(token) || wsToken == null || !room.equals(wsToken.getRoomId())) {
            handler.sendEvent("error", "Invalid token");
            ctx.closeSession(CloseStatus.BAD_DATA, "Invalid token");
            return;
        }

        if (!clients.containsKey(room)) {
            clients.put(room, new ConcurrentHashMap<>());
        }

        clients.get(room).put(ctx.getSessionId(), handler);
        handler.handle();
    }

    private static void onMessage(WsMessageContext ctx, WsRoute handler) {
        handler.handleMessage(ctx);
    }

    private static void onClose(WsContext ctx, WsRoute handler) {
        String room = handler.getRoom();
        String id = ctx.getSessionId();

        if (clients.containsKey(room)) {
            clients.get(room).remove(id);
            if (clients.get(room).isEmpty()) {
                clients.remove(room);
            }
        }
    }
}
