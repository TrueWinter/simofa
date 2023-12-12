package dev.truewinter.simofa.routes.api;

import dev.truewinter.simofa.DeploymentServer;
import dev.truewinter.simofa.RouteLoader;
import dev.truewinter.simofa.routes.Route;
import io.javalin.http.Context;

import java.sql.SQLException;
import java.util.*;

@SuppressWarnings("unused")
@RouteLoader.RouteClass()
public class DeploymentServersAPIRoute extends Route {
    @RouteLoader.RouteInfo(
            url = "/api/deployment-servers"
    )
    public void get(Context ctx) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("servers", new ArrayList<>());

        try {
            List<DeploymentServer> deploymentServers = getDatabase().getDeploymentServerDatabase().getDeploymentServers();
            resp.put("servers", deploymentServers);
        } catch (SQLException e) {
            e.printStackTrace();
            resp.put("success", false);
        }

        ctx.json(resp);
    }
}
