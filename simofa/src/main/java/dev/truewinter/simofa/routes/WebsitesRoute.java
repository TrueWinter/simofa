package dev.truewinter.simofa.routes;

import io.javalin.http.Context;

public class WebsitesRoute extends Route {
    @Override
    public void get(Context ctx) {
        render(ctx, "websites/list");
    }
}
