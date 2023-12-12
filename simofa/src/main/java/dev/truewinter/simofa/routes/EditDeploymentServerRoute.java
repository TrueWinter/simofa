package dev.truewinter.simofa.routes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import dev.truewinter.simofa.DeploymentServer;
import dev.truewinter.simofa.RouteLoader;
import dev.truewinter.simofa.formvalidators.AddEditDeploymentServerValidator;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Optional;

@SuppressWarnings("unused")
@RouteLoader.RouteClass()
public class EditDeploymentServerRoute extends Route {
    private HashMap<String, Object> getDeploymentServerDataForModel(DeploymentServer d) throws JsonProcessingException {
        // This configuration makes it easier to get the
        // relevant data in the Pebble template.
        HashMap<String, Object> containerMap = new HashMap<>();
        ObjectMapper objectMapper = JsonMapper.builder()
                .configure(MapperFeature.USE_ANNOTATIONS, false)
                .build();
        String deploymentServerJson = objectMapper.writeValueAsString(d);
        @SuppressWarnings("unchecked")
        HashMap<Object, Object> deploymentServerData = objectMapper.readValue(deploymentServerJson, HashMap.class);

        containerMap.put("server", deploymentServerData);

        return containerMap;
    }

    @RouteLoader.RouteInfo(
            url = "/deployment-servers/{id}/edit"
    )
    public void get(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        try {
            Optional<DeploymentServer> deploymentServer = getDatabase().getDeploymentServerDatabase().getDeploymentServer(id);
            deploymentServer.ifPresentOrElse(d -> {
                try {
                    render(ctx, "deployment-servers/edit", getDeploymentServerDataForModel(d));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                    ctx.status(500).result("Failed to get deployment server data");
                }
            }, () -> {
                ctx.status(404).result("Deployment server does not exist");
            });
        } catch (SQLException e) {
            e.printStackTrace();
            ctx.status(500).result("An error occurred");
        }
    }

    @RouteLoader.RouteInfo(
            url = "/deployment-servers/{id}/edit",
            method = HandlerType.POST
    )
    public void post(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));

        try {
            Optional<DeploymentServer> deploymentServer = getDatabase().getDeploymentServerDatabase().getDeploymentServer(id);
            if (deploymentServer.isEmpty()) {
                ctx.status(404).result("Deployment server does not exist");
                return;
            }

            String name = ctx.formParam("name");
            String url = ctx.formParam("url");
            String key = ctx.formParam("key");

            Optional<String> error = new AddEditDeploymentServerValidator().hasError(ctx);
            if (error.isPresent()) {
                renderError(ctx, "deployment-servers/add", error.get(), getDeploymentServerDataForModel(deploymentServer.get()));
                return;
            }

            DeploymentServer deploymentServer1 = new DeploymentServer(id, name, url, key);
            getDatabase().getDeploymentServerDatabase().editDeploymentServer(deploymentServer1);
            renderSuccess(ctx, "deployment-servers/edit", "Edited deployment server", getDeploymentServerDataForModel(getDatabase().getDeploymentServerDatabase().getDeploymentServer(id).get()));
        } catch (Exception e) {
            e.printStackTrace();
            ctx.status(500).result("Failed to edit deployment server");
        }
    }
}
