package dev.truewinter.simofa.routes.api;

import dev.truewinter.simofa.RouteLoader;
import dev.truewinter.simofa.Simofa;
import dev.truewinter.simofa.api.WebsiteBuild;
import dev.truewinter.simofa.routes.Route;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.http.NotFoundResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("unused")
@RouteLoader.RouteClass
public class BuildsRoute extends Route {
    @RouteLoader.RouteInfo(
            url = "/api/websites/{websiteId}/builds/{buildId}",
            method = HandlerType.DELETE
    )
    public void delete(Context ctx) {
        String websiteId = ctx.pathParam("websiteId");
        String buildId = ctx.pathParam("buildId");

        HashMap<String, List<WebsiteBuild>> allBuildsList = Simofa.getBuildQueueManager().getBuildQueue()
                .getWebsiteBuildList();

        if (!allBuildsList.containsKey(websiteId)) {
            throw new NotFoundResponse("Website not found");
        }

        List<WebsiteBuild> buildList = allBuildsList.get(websiteId);
        Optional<WebsiteBuild> build = buildList.stream().filter(b -> b.getId().equals(buildId)).findFirst();
        if (build.isEmpty()) {
            throw new NotFoundResponse("Build not found");
        }

        Simofa.getBuildQueueManager().getBuildQueue().remove(build.get().getWebsite());
    }
}
