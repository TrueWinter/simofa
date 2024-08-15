package dev.truewinter.simofa.routes;

import dev.truewinter.simofa.api.DeployServer;
import dev.truewinter.simofa.RouteLoader;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;

import java.util.Optional;

@SuppressWarnings("unused")
@RouteLoader.RouteClass()
public class DeleteDeploymentServerRoute extends Route {
    @RouteLoader.RouteInfo(
            url = "/deployment-servers/{id}/delete",
            method = HandlerType.POST
    )
    public void post(Context ctx) {
        String id = ctx.pathParam("id");

        try {
            Optional<DeployServer> w = getDatabase().getDeployServerDatabase().getDeployServer(id);
            if (w.isPresent()) {
                getDatabase().getDeployServerDatabase().deleteDeployServer(id);
                redirect(ctx,"/deployment-servers");
            } else {
                ctx.status(404).result("Deployment server does not exist");
            }
        } catch (Exception e) {
            e.printStackTrace();
            ctx.status(500).result("Failed to delete deployment server: " + e.getMessage());
        }
    }
}
