package dev.truewinter.simofa.routes.api;

import dev.truewinter.simofa.Simofa;
import dev.truewinter.simofa.RouteLoader;
import dev.truewinter.simofa.api.DeployServer;
import dev.truewinter.simofa.api.Website;
import dev.truewinter.simofa.routes.Route;
import io.javalin.http.*;

import java.sql.SQLException;
import java.util.*;

@SuppressWarnings("unused")
@RouteLoader.RouteClass()
public class DeployServersAPIRoute extends Route {
    @RouteLoader.RouteInfo(
            url = "/api/deploy-servers"
    )
    public void get(Context ctx) {
        try {
            ctx.json(getDatabase().getDeployServerDatabase().getDeployServers());
        } catch (SQLException e) {
            Simofa.getLogger().error("Failed to get deploy servers", e);
            throw new InternalServerErrorResponse("Failed to get deploy servers");
        }
    }

    @RouteLoader.RouteInfo(
            url = "/api/deploy-servers/{id}"
    )
    public void getOneDeployServer(Context ctx) {
        String id = ctx.pathParam("id");

        try {
            Optional<DeployServer> website = getDatabase().getDeployServerDatabase().getDeployServer(id);
            website.ifPresentOrElse(ctx::json, () -> {
                throw new NotFoundResponse("Deploy server not found");
            });
        } catch (SQLException e) {
            Simofa.getLogger().error("Failed to get deploy server", e);
            throw new InternalServerErrorResponse("Failed to get deploy server");
        }
    }

    @RouteLoader.RouteInfo(
            url = "/api/deploy-servers",
            method = HandlerType.POST
    )
    public void addDeployServer(Context ctx) throws Exception {
        DeployServer deployServer = ctxToT(ctx, DeployServer.class, null);

        try {
            String id = getDatabase().getDeployServerDatabase().addDeployServer(deployServer);

            HashMap<String, String> resp = new HashMap<>();
            resp.put("id", id);
            ctx.json(resp);
        } catch (SQLException e) {
            Simofa.getLogger().error("Failed to add deploy server", e);
            throw new InternalServerErrorResponse("Failed to add deploy server");
        }
    }

    @RouteLoader.RouteInfo(
            url = "/api/deploy-servers/{id}",
            method = HandlerType.PUT
    )
    public void editDeployServer(Context ctx) throws Exception {
        String id = ctx.pathParam("id");
        DeployServer deployServer = ctxToT(ctx, DeployServer.class, id);

        try {
            getDatabase().getDeployServerDatabase().editDeployServer(deployServer);

            HashMap<String, Boolean> resp = new HashMap<>();
            resp.put("success", true);
            ctx.json(resp);
        } catch (SQLException e) {
            Simofa.getLogger().error("Failed to edit deploy server", e);
            throw new InternalServerErrorResponse("Failed to edit deploy server");
        }
    }

    @RouteLoader.RouteInfo(
            url = "/api/deploy-servers/{id}",
            method = HandlerType.DELETE
    )
    public void deleteDeployServer(Context ctx) {
        String id = ctx.pathParam("id");

        try {
            getDatabase().getDeployServerDatabase().deleteDeployServer(id);

            HashMap<String, Boolean> resp = new HashMap<>();
            resp.put("success", true);
            ctx.json(resp);
        } catch (SQLException e) {
            Simofa.getLogger().error("Failed to delete deploy server", e);
            throw new InternalServerErrorResponse("Failed to delete deploy server");
        }
    }
}
