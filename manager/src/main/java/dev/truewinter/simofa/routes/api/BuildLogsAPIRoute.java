package dev.truewinter.simofa.routes.api;

import dev.truewinter.simofa.RouteLoader;
import dev.truewinter.simofa.Simofa;
import dev.truewinter.simofa.common.SimofaLog;
import dev.truewinter.simofa.common.Util;
import dev.truewinter.simofa.api.WebsiteBuild;
import dev.truewinter.simofa.routes.Route;
import io.javalin.http.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@RouteLoader.RouteClass()
public class BuildLogsAPIRoute extends Route {
    @RouteLoader.RouteInfo(
            url = "/api/websites/{wid}/build/{bid}/logs"
    )
    public void get(Context ctx) {
        String websiteId = ctx.pathParam("wid");
        String buildId = ctx.pathParam("bid");
        String after = ctx.queryParam("after");

        ctx.json(getLogs(websiteId, buildId, after));
    }

    protected static HashMap<String, Object> getLogs(String websiteId, String buildId, String after) {
        HashMap<String, List<WebsiteBuild>> allBuildsList = Simofa.getBuildQueueManager().getBuildQueue().getWebsiteBuildList();

        if (!allBuildsList.containsKey(websiteId)) {
            return errorResponse("No builds found for that website");
        }

        List<WebsiteBuild> buildList = allBuildsList.get(websiteId);
        Optional<WebsiteBuild> build = buildList.stream().filter(b -> b.getId().equals(buildId)).findFirst();
        if (build.isEmpty()) {
            return errorResponse("Build not found");
        }

        List<SimofaLog> logs;
        try {
            if (!Util.isBlank(after)) {
                logs = build.get().getLogs().stream()
                        .filter(l -> l.getTimestamp() > Long.parseLong(after))
                        .collect(Collectors.toCollection(ArrayList::new));
            } else {
                logs = build.get().getLogs();
            }
        } catch (Exception ignored) {
            logs = build.get().getLogs();
        }

        logs.sort((a, b) -> {
            if (a.getTimestamp() == b.getTimestamp()) return 0;
            return a.getTimestamp() < b.getTimestamp() ? -1 : 1;
        });

        return successResponse(build.get(), logs);
    }

    private static HashMap<String, Object> errorResponse(String error) {
        HashMap<String, Object> resp = new HashMap<>();
        resp.put("success", false);
        resp.put("error", error);
        return resp;
    }

    protected static HashMap<String, Object> successResponse(WebsiteBuild build, List<SimofaLog> logs) {
        HashMap<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("status", build.getStatus());
        resp.put("duration", build.getRunTime());
        resp.put("logs", logs);
        return resp;
    }
}
