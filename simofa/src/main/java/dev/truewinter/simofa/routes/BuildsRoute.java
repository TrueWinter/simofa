package dev.truewinter.simofa.routes;

import dev.truewinter.simofa.RouteLoader;
import io.javalin.http.Context;

@SuppressWarnings("unused")
@RouteLoader.RouteClass()
public class BuildsRoute extends Route {
    @RouteLoader.RouteInfo(
            url = "/builds/*"
    )
    public void get(Context ctx) {
        render(ctx, "builds/app");
    }
}
