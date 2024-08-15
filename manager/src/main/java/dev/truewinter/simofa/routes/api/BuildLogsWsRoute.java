package dev.truewinter.simofa.routes.api;

import dev.truewinter.simofa.Simofa;
import dev.truewinter.simofa.api.WebsiteBuild;
import dev.truewinter.simofa.api.internal.WsRegistry;
import dev.truewinter.simofa.common.SimofaLog;
import dev.truewinter.simofa.common.Util;
import dev.truewinter.simofa.routes.WsRoute;
import io.javalin.websocket.WsContext;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BuildLogsWsRoute extends WsRoute {
    private static final ConcurrentHashMap<String, ConcurrentHashMap<String, LastStatus>> lastStatus = new ConcurrentHashMap<>();

    public BuildLogsWsRoute(WsContext ctx) {
        super(ctx);
    }

    @Override
    public void handle() {
        String websiteId = ctx.pathParam("websiteId");
        String buildId = ctx.pathParam("buildId");
        String after = ctx.queryParam("after");

        List<WebsiteBuild> builds = Simofa.getBuildQueueManager().getBuildQueue().getWebsiteBuildList().get(websiteId);
        if (builds == null || builds.isEmpty()) {
            sendEvent("error", "No builds found for that website");
            ctx.closeSession();
            return;
        }

        Optional<WebsiteBuild> build = builds.stream().filter(b -> b.getId().equals(buildId)).findFirst();
        if (build.isEmpty()) {
            sendEvent("error", "Build not found");
            ctx.closeSession();
            return;
        } else if (WebsiteBuild.END_STATUSES.contains(build.get().getStatus())) {
            sendEvent("error", "Build is already finished");
            ctx.closeSession();
            return;
        }

        if (!Util.isBlank(after)) {
            sendEvent("logs", BuildLogsAPIRoute.getLogs(websiteId, buildId, after));
        }
    }

    @Override
    public String getRoom() {
        return formatRoomKey(ctx.pathParam("websiteId"), ctx.pathParam("buildId"));
    }

    private static String formatRoomKey(String websiteId, String buildId) {
        return String.format("%s-%s-%s", WsRegistry.Instances.WEBSITE_LOGS, websiteId, buildId);
    }

    public static void sendNewLog(Object audience, Object data) {
        if (!(audience instanceof WebsiteBuild build)) {
            throw new IllegalArgumentException("Audience must be of type WebsiteBuild");
        }

        if (!(data instanceof SimofaLog log)) {
            throw new IllegalArgumentException("Log must be of type SimofaLog");
        }

        String room = formatRoomKey(build.getWebsite().getId(), build.getId());
        ConcurrentHashMap<String, WsRoute> clients = getClients(room);
        if (clients == null) return;

        List<SimofaLog> logs = List.of(log);
        clients.forEach((id, handler) -> {
            handler.sendEvent("logs", BuildLogsAPIRoute.successResponse(build, logs));

            if (!lastStatus.containsKey(room)) {
                lastStatus.put(room, new ConcurrentHashMap<>());
            }
            lastStatus.get(room).put(id, new LastStatus(build, System.currentTimeMillis()));
        });
    }

    @Override
    public void closeInactiveConnections() {
        ConcurrentHashMap<String, WsRoute> clients = getClients();
        if (clients == null) return;

        ConcurrentHashMap<String, LastStatus> ls = lastStatus.get(getRoom());
        if (ls == null) return;

        for (Iterator<Map.Entry<String, LastStatus>> it = ls.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, LastStatus> entry = it.next();
            String id = entry.getKey();
            LastStatus l = entry.getValue();

            // Wait 30 seconds after build has ended to let last logs come in
            if (WebsiteBuild.END_STATUSES.contains(l.build.getStatus()) &&
                    System.currentTimeMillis() - l.timestamp >= 30 * 1000) {
                WsRoute handler = clients.get(id);
                if (handler != null) {
                    handler.sendEvent("error", "Inactive connection");
                }
            }

            if (!clients.containsKey(id)) {
                it.remove();
            }
        }
    }

    private record LastStatus(WebsiteBuild build, Long timestamp) {}
}
