package dev.truewinter.simofa.routes;

import io.javalin.http.Context;

public class BuildsRoute extends Route {
    @Override
    public void get(Context ctx) {
        render(ctx, "builds/app");
    }
}
