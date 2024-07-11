package dev.truewinter.simofa.routes.api;

import dev.truewinter.simofa.Simofa;
import dev.truewinter.simofa.api.WebsiteBuild;
import dev.truewinter.simofa.api.internal.SseRegistry;
import dev.truewinter.simofa.common.BuildStatus;
import dev.truewinter.simofa.common.SimofaLog;
import dev.truewinter.simofa.routes.SseRoute;
import io.javalin.http.Context;
import io.javalin.http.sse.SseClient;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BuildLogsSseRoute extends SseRoute {
    private static final HashMap<String, Queue<SseClient>> clients = new HashMap<>();
    private static final HashMap<SseClient, LastStatus> lastStatus = new HashMap<>();

    public static void init() {
        SseRegistry.registerSseConsumer(SseRegistry.Instances.WEBSITE_LOGS, BuildLogsSseRoute::sendNewLog);
    }

    public static void ping() {
        ping(clients);
        closeInactiveConnections();
    }

    public static void sse(SseClient client) {
        Context ctx = client.ctx();
        String id = String.format("%s-%s", ctx.pathParam("wid"), ctx.pathParam("bid"));

        List<WebsiteBuild> builds = Simofa.getBuildQueueManager().getBuildQueue().getWebsiteBuildList()
                .get(Integer.parseInt(ctx.pathParam("wid")));
        if (builds == null || builds.isEmpty()) {
            client.sendEvent("error", "No builds found for that website");
            return;
        }
        Optional<WebsiteBuild> build = builds.stream().filter(b -> b.getId().equals(ctx.pathParam("bid"))).findFirst();
        if (build.isEmpty()) {
            client.sendEvent("error", "Build not found");
            return;
        } else if (WebsiteBuild.END_STATUSES.contains(build.get().getStatus())) {
            client.sendEvent("error", "Build is already finished");
            return;
        }

        if (!clients.containsKey(id)) {
            clients.put(id, new ConcurrentLinkedQueue<>());
        }
        clients.get(id).add(client);

        client.keepAlive();
        client.onClose(() -> {
            clients.get(id).remove(client);
            System.out.println("Removed client " + client);
        });
    }

    public static void sendNewLog(WebsiteBuild build, Object log) {
        if (!(log instanceof SimofaLog)) {
            throw new IllegalArgumentException("Log must be of type SimofaLog");
        }

        String id = String.format("%d-%s", build.getWebsite().getId(), build.getId());
        if (!clients.containsKey(id)) return;

        List<SimofaLog> logs = List.of((SimofaLog) log);
        clients.get(id).forEach(client -> {
            client.sendEvent(BuildLogsAPIRoute.successResponse(build, logs));
            lastStatus.put(client, new LastStatus(build, System.currentTimeMillis()));
        });
    }

    private static void closeInactiveConnections() {
        for (Iterator<Map.Entry<SseClient, LastStatus>> it = lastStatus.entrySet().iterator(); it.hasNext();) {
            Map.Entry<SseClient, LastStatus> entry = it.next();
            SseClient c = entry.getKey();
            LastStatus l = entry.getValue();

            if (WebsiteBuild.END_STATUSES.contains(l.build.getStatus()) &&
                    System.currentTimeMillis() - l.timestamp >= 30 * 1000) {
                c.sendEvent("error", "Inactive connection");
                c.close();
                System.out.println("Inactive client " + c);
            }

            String id = String.format("%s-%s", l.build.getWebsite().getId(), l.build.getId());
            Queue<SseClient> clients1 = clients.get(id);
            if (clients1 != null && !clients1.contains(c)) {
                it.remove();
                System.out.println("Removed from last status " + c);
            }
        }
    }

    private record LastStatus(WebsiteBuild build, Long timestamp) {};
}
