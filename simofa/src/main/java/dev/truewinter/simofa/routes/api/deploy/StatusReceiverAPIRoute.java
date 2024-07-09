package dev.truewinter.simofa.routes.api.deploy;

import dev.truewinter.simofa.RouteLoader;
import dev.truewinter.simofa.Simofa;
import dev.truewinter.simofa.common.BuildStatus;
import dev.truewinter.simofa.api.WebsiteBuild;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;

import java.util.HashMap;

@SuppressWarnings("unused")
@RouteLoader.RouteClass(
        verifyLogin = false,
        verifyCsrf = false
)
public class StatusReceiverAPIRoute extends FromDeploymentServerAPIRoute {
    @RouteLoader.RouteInfo(
            url = "/api/websites/{wid}/build/{bid}/status/set",
            method = HandlerType.POST
    )
    public void post(Context ctx) {
        String key = ctx.header("key");
        WebsiteBuild build = getBuild(ctx);

        HashMap<String, Object> resp = new HashMap<>();
        resp.put("success", true);

        if (build == null) {
            resp.put("success", false);
            resp.put("error", "Build does not exist");
        } else {
            try {
                if (key == null || key.isBlank() || !isAuthorized(build, key)) {
                    resp.put("error", "Incorrect key");
                } else {
                    String status = ctx.body();
                    build.setStatus(BuildStatus.valueOf(status));
                }
            } catch (Exception e) {
                Simofa.getLogger().error("An error occurred while processing deployment status", e);
                resp.put("error", e.getMessage());
            }
        }

        ctx.json(resp);
    }
}
