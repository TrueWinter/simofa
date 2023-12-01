package dev.truewinter.simofa.routes.api;

import dev.truewinter.simofa.Simofa;
import dev.truewinter.simofa.common.SimofaLog;
import dev.truewinter.simofa.common.Util;
import dev.truewinter.simofa.docker.WebsiteBuild;
import dev.truewinter.simofa.routes.Route;
import io.javalin.http.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class BuildLogsAPIRoute extends Route {
    @Override
    public void get(Context ctx) {
        int websiteId = Integer.parseInt(ctx.pathParam("wid"));
        String buildId = ctx.pathParam("bid");
        String after = ctx.queryParam("after");

        HashMap<Integer, List<WebsiteBuild>> allBuildsList = Simofa.getBuildQueueManager().getBuildQueue().getWebsiteBuildList();

        HashMap<String, Object> resp = new HashMap<>();
        resp.put("success", true);

        if (!allBuildsList.containsKey(websiteId)) {
            resp.put("success", false);
            resp.put("error", "No builds found for that website");
            ctx.status(404).json(resp);
            return;
        }

        List<WebsiteBuild> buildList = allBuildsList.get(websiteId);
        Optional<WebsiteBuild> build = buildList.stream().filter(b -> b.getId().equals(buildId)).findFirst();
        if (build.isEmpty()) {
            resp.put("success", false);
            resp.put("error", "Build not found");
            ctx.status(404).json(resp);
            return;
        }

        List<SimofaLog> logs;
        try {
            if (!Util.isBlank(after)) {
                logs = build.get().getLogs().stream()
                        .filter(l -> l.getTimestamp() > Long.parseLong(after)).toList();
            } else {
                logs = build.get().getLogs();
            }
        } catch (Exception ignored) {
            logs = build.get().getLogs();
        }

        resp.put("status", build.get().getStatus());
        resp.put("duration", build.get().getRunTime());
        resp.put("logs", logs);
        ctx.json(resp);
    }
}
