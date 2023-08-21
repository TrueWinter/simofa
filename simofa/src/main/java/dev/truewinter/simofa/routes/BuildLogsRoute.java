package dev.truewinter.simofa.routes;

import dev.truewinter.simofa.Simofa;
import dev.truewinter.simofa.docker.WebsiteBuild;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

public class BuildLogsRoute extends Route {
    @Override
    public void get(Context ctx) {
        int websiteId = Integer.parseInt(ctx.pathParam("wid"));
        String buildId = ctx.pathParam("bid");
        HashMap<Integer, List<WebsiteBuild>> allBuildsList = Simofa.getBuildQueueManager().getBuildQueue().getWebsiteBuildList();

        if (!allBuildsList.containsKey(websiteId)) {
            ctx.status(404).result("No builds found for that website");
            return;
        }

        List<WebsiteBuild> buildList = allBuildsList.get(websiteId);
        Optional<WebsiteBuild> build = buildList.stream().filter(b -> b.getId().equals(buildId)).findFirst();
        if (build.isEmpty()) {
            ctx.status(404).result("Build not found");
            return;
        }

        HashMap<String, Object> model = new HashMap<>();
        model.put("website_id", websiteId);
        model.put("build_id", buildId);

        render(ctx, "websites/logs", model);
    }
}
