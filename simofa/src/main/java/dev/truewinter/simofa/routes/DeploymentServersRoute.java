package dev.truewinter.simofa.routes;

import io.javalin.http.Context;

public class DeploymentServersRoute extends Route {
    @Override
    public void get(Context ctx) {
        render(ctx, "deployment-servers/list");
    }
}
