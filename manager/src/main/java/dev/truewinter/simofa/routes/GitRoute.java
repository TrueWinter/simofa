package dev.truewinter.simofa.routes;

import dev.truewinter.simofa.RouteLoader;
import io.javalin.http.Context;

@SuppressWarnings("unused")
@RouteLoader.RouteClass()
public class GitRoute extends Route {
    @RouteLoader.RouteInfo(
            url = "/git"
    )
    public void get(Context ctx) {
        render(ctx, "git/list");
    }
}
