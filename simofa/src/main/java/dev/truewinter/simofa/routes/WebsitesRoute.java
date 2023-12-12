package dev.truewinter.simofa.routes;

import dev.truewinter.simofa.RouteLoader;
import io.javalin.http.Context;

@SuppressWarnings("unused")
@RouteLoader.RouteClass()
public class WebsitesRoute extends Route {
    @RouteLoader.RouteInfo(
            url = "/websites"
    )
    public void get(Context ctx) {
        render(ctx, "websites/list");
    }
}
