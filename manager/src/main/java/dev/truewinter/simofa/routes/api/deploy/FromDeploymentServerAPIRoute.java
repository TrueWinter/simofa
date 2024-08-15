package dev.truewinter.simofa.routes.api.deploy;

import dev.truewinter.simofa.api.DeployServer;
import dev.truewinter.simofa.Simofa;
import dev.truewinter.simofa.common.Util;
import dev.truewinter.simofa.api.WebsiteBuild;
import dev.truewinter.simofa.routes.Route;
import io.javalin.http.Context;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public abstract class FromDeploymentServerAPIRoute extends Route {
    @Nullable
    public WebsiteBuild getBuild(Context ctx) {
        String websiteId = ctx.pathParam("wid");
        String buildId = ctx.pathParam("bid");

        List<WebsiteBuild> builds = Simofa.getBuildQueueManager().getBuildQueue().getWebsiteBuildList().get(websiteId);

        if (builds == null) {
            return null;
        }

        WebsiteBuild build = null;

        for (WebsiteBuild b : builds) {
            if (b.getWebsite().getId().equals(websiteId) && b.getId().equals(buildId)) {
                build = b;
                break;
            }
        }

        return build;
    }

    public boolean isAuthorized(WebsiteBuild websiteBuild, String key) throws Exception {
        Optional<DeployServer> deploymentServer = getDatabase().getDeployServerDatabase()
                .getDeployServer(websiteBuild.getWebsite().getDeployServer());
        if (deploymentServer.isEmpty()) {
            throw new Exception("Deployment server not found");
        }

        return Util.secureCompare(deploymentServer.get().getKey(), key);
    }
}
