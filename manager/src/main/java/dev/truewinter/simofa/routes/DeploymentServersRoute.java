package dev.truewinter.simofa.routes;

import dev.truewinter.simofa.RouteLoader;
import io.javalin.http.Context;

@SuppressWarnings("unused")
@RouteLoader.RouteClass()
public class DeploymentServersRoute extends Route {
    @RouteLoader.RouteInfo(
            url = "/deployment-servers"
    )
    public void get(Context ctx) {
        render(ctx, "deployment-servers/list");
    }
}
