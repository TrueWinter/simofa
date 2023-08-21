package dev.truewinter.simofa.routes;

import io.javalin.http.Context;

public class DockerRoute extends Route {
    @Override
    public void get(Context ctx) {
        render(ctx, "docker/app");
    }
}
