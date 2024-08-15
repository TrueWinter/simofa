package dev.truewinter.simofa.routes.api;

import dev.truewinter.simofa.Simofa;
import dev.truewinter.simofa.api.WebsiteBuild;
import dev.truewinter.simofa.api.internal.WsRegistry;
import dev.truewinter.simofa.common.Util;
import dev.truewinter.simofa.routes.WsRoute;
import io.javalin.websocket.WsContext;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QueueWsRoute extends WsRoute {
    public QueueWsRoute(WsContext ctx) {
        super(ctx);
    }

    @Override
    public void handle() {
        sendEvent("queue", getQueue(ctx.queryParam("website")));
    }

    @Override
    public String getRoom() {
        return formatRoomKey(ctx.queryParam("website"));
    }

    private static String formatRoomKey(@Nullable String websiteId) {
        if (Util.isBlank(websiteId)) {
            return WsRegistry.Instances.BUILD_QUEUE.toString();
        }

        return String.format("%s-%s", WsRegistry.Instances.BUILD_QUEUE, websiteId);
    }

    public static void sendUpdate(Object audience, Object ignored) {
        if (!(audience instanceof String website)) {
            throw new IllegalArgumentException("Audience must be of type String");
        }

        List<WebsiteBuild> queue = getQueue(website);
        String room = formatRoomKey(website);
        sendToRoom(room, queue);
        if (!room.equals(formatRoomKey(null))) {
            sendToRoom(formatRoomKey(null), getQueue(null));
        }
    }

    private static List<WebsiteBuild> getQueue(@Nullable String websiteId) {
        List<WebsiteBuild> builds = new ArrayList<>();

        if (Util.isBlank(websiteId)) {
            Simofa.getBuildQueueManager().getBuildQueue().getWebsiteBuildList()
                    .values().forEach(builds::addAll);
        } else {
            List<WebsiteBuild> b = Simofa.getBuildQueueManager().getBuildQueue().getWebsiteBuildList()
                    .getOrDefault(websiteId, new ArrayList<>());
            builds.addAll(b);
        }

        List<WebsiteBuild> copiedList = new ArrayList<>(builds);
        QueueAPIRoute.sort(copiedList);
        System.out.println(copiedList);
        return copiedList;
    }

    private static void sendToRoom(String room, List<WebsiteBuild> queue) {
        ConcurrentHashMap<String, WsRoute> clients = getClients(room);
        if (clients == null) return;

        clients.forEach((id, handler) -> {
            handler.sendEvent("queue", queue);
        });
    }
}
