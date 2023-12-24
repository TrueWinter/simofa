package dev.truewinter.simofa.routes.api.deploy;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.truewinter.simofa.RouteLoader;
import dev.truewinter.simofa.Simofa;
import dev.truewinter.simofa.common.SimofaLog;
import dev.truewinter.simofa.api.WebsiteBuild;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;

import java.util.HashMap;

@SuppressWarnings("unused")
@RouteLoader.RouteClass(
        verifyLogin = false,
        verifyCsrf = false
)
public class LogReceiverAPIRoute extends FromDeploymentServerAPIRoute {
    @RouteLoader.RouteInfo(
            url = "/api/websites/{wid}/build/{bid}/logs/add",
            method = HandlerType.POST
    )
    public void post(Context ctx) {
        String key = ctx.header("key");
        WebsiteBuild build = getBuild(ctx);

        HashMap<String, Object> resp = new HashMap<>();
        resp.put("success", true);

        if (build == null) {
            resp.put("success", false);
            resp.put("error", "Build does not exist");
        } else {
            try {
                if (key == null || key.isBlank() || !isAuthorized(build, key)) {
                    resp.put("error", "Incorrect key");
                } else {
                    String json = ctx.body();
                    ObjectMapper objectMapper = new ObjectMapper();
                    SimofaLog[] logs = objectMapper.readValue(json, SimofaLog[].class);
                    for (SimofaLog log : logs) {
                        build.addLog(log);
                    }
                }
            } catch (Exception e) {
                Simofa.getLogger().error("An error occurred while processing logs", e);
                resp.put("error", e.getMessage());
            }
        }

        ctx.json(resp);
    }
}
