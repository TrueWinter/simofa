package dev.truewinter.simofa.routes;

import io.javalin.http.Context;

public class GitRoute extends Route {
    @Override
    public void get(Context ctx) {
        render(ctx, "git/list");
    }
}
