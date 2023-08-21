package dev.truewinter.simofa.routes;

import dev.truewinter.simofa.DeploymentServer;
import io.javalin.http.Context;

import java.util.Optional;

public class DeleteDeploymentServerRoute extends Route {
    @Override
    public void post(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));

        try {
            Optional<DeploymentServer> w = getDatabase().getDeploymentServerDatabase().getDeploymentServer(id);
            if (w.isPresent()) {
                getDatabase().getDeploymentServerDatabase().deleteDeploymentServer(id);
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
