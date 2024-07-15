package dev.truewinter.simofa.routes;

import dev.truewinter.simofa.RouteLoader;
import dev.truewinter.simofa.Simofa;
import dev.truewinter.simofa.common.Util;
import dev.truewinter.simofa.api.WebsiteBuild;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("unused")
@RouteLoader.RouteClass()
public class StopBuildRoute extends Route {
    @RouteLoader.RouteInfo(
            url = "/websites/{wid}/build/{bid}/stop",
            method = HandlerType.POST
    )
    public void post(Context ctx) {
        String websiteId = ctx.pathParam("wid");
        String buildId = ctx.pathParam("bid");
        HashMap<String, List<WebsiteBuild>> allBuildsList = Simofa.getBuildQueueManager().getBuildQueue().getWebsiteBuildList();

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

        Simofa.getBuildQueueManager().getBuildQueue().remove(build.get().getWebsite());

        String redirectTo = ctx.queryParam("redirectTo");
        if (Util.isBlank(redirectTo)) {
            redirectTo = String.format("/websites/%s/logs", websiteId);
        }

        redirect(ctx, redirectTo);
    }
}
