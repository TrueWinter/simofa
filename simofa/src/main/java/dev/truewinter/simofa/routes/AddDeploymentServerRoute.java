package dev.truewinter.simofa.routes;

import dev.truewinter.simofa.DeploymentServer;
import dev.truewinter.simofa.RouteLoader;
import dev.truewinter.simofa.formvalidators.AddEditDeploymentServerValidator;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;

import java.sql.SQLException;
import java.util.Optional;

@SuppressWarnings("unused")
@RouteLoader.RouteClass(
        csrfErrorPage = "deployment-servers/add"
)
public class AddDeploymentServerRoute extends Route {
    @RouteLoader.RouteInfo(
            url = "/deployment-servers/add"
    )
    public void get(Context ctx) {
        render(ctx, "deployment-servers/add");
    }

    @RouteLoader.RouteInfo(
            url = "/deployment-servers/add",
            method = HandlerType.POST
    )
    public void post(Context ctx) {
        String name = ctx.formParam("name");
        String url = ctx.formParam("url");
        String key = ctx.formParam("key");

        Optional<String> error = new AddEditDeploymentServerValidator().hasError(ctx);
        if (error.isPresent()) {
            renderError(ctx, "deployment-servers/add", error.get());
            return;
        }

        DeploymentServer deploymentServer = new DeploymentServer(0, name, url, key);
        try {
            getDatabase().getDeploymentServerDatabase().addDeploymentServer(deploymentServer);
            redirect(ctx, "/deployment-servers");
        } catch (SQLException e) {
            e.printStackTrace();
            renderError(ctx, "deployment-servers/add", "Failed to add deployment server");
        }
    }
}
