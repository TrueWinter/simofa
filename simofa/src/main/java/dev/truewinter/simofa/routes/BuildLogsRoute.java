package dev.truewinter.simofa.routes;

import dev.truewinter.simofa.RouteLoader;
import dev.truewinter.simofa.Simofa;
import dev.truewinter.simofa.api.WebsiteBuild;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("unused")
@RouteLoader.RouteClass()
public class BuildLogsRoute extends Route {
    @RouteLoader.RouteInfo(
            url = "/websites/{wid}/build/{bid}/logs"
    )
    public void get(Context ctx) {
        int websiteId = Integer.parseInt(ctx.pathParam("wid"));
        String buildId = ctx.pathParam("bid");
        HashMap<Integer, List<WebsiteBuild>> allBuildsList = Simofa.getBuildQueueManager().getBuildQueue().getWebsiteBuildList();

        if (!allBuildsList.containsKey(websiteId)) {
            ctx.redirect("/websites/" + websiteId + "/logs");
            return;
        }

        List<WebsiteBuild> buildList = allBuildsList.get(websiteId);
        Optional<WebsiteBuild> build = buildList.stream().filter(b -> b.getId().equals(buildId)).findFirst();
        if (build.isEmpty()) {
            ctx.redirect("/websites/" + websiteId + "/logs");
            return;
        }

        HashMap<String, Object> model = new HashMap<>();
        model.put("website_id", websiteId);
        model.put("build_id", buildId);

        render(ctx, "websites/logs", model);
    }
}
