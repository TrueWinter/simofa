package dev.truewinter.simofa.routes.api;

import dev.truewinter.simofa.api.WebsiteBuild;
import dev.truewinter.simofa.common.SimofaLog;
import dev.truewinter.simofa.routes.Route;
import io.javalin.http.Context;
import io.javalin.http.sse.SseClient;

import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BuildLogsSseRoute extends Route {
    private static final HashMap<String, Queue<SseClient>> clients = new HashMap<>();

    public static void init() {
        WebsiteBuild.internal_registerSseConsumer(BuildLogsSseRoute::sendNewLog);
    }

    public static void sse(SseClient client) {
        Context ctx = client.ctx();
        String id = String.format("%s-%s", ctx.pathParam("wid"), ctx.pathParam("bid"));

        if (!clients.containsKey(id)) {
            clients.put(id, new ConcurrentLinkedQueue<>());
        }
        clients.get(id).add(client);

        client.keepAlive();
        client.onClose(() -> clients.get(id).remove(client));
    }

    public static void sendNewLog(WebsiteBuild build, SimofaLog log) {
        String id = String.format("%d-%s", build.getWebsite().getId(), build.getId());
        if (!clients.containsKey(id)) return;

        List<SimofaLog> logs = List.of(log);
        clients.get(id).forEach(client -> {
            client.sendEvent(BuildLogsAPIRoute.successResponse(build, logs));
        });
    }
}
