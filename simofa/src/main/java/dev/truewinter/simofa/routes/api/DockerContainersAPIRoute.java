package dev.truewinter.simofa.routes.api;

import dev.truewinter.simofa.Simofa;
import dev.truewinter.simofa.routes.Route;
import io.javalin.http.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DockerContainersAPIRoute extends Route {
    @Override
    public void get(Context ctx) {
        List<HashMap<String, String>> containers = new ArrayList<>();
        Simofa.getDockerManager().getContainers().forEach(c -> {
            HashMap<String, String> container = new HashMap<>();
            container.put("id", c.getId());
            container.put("name", c.getName());
            container.put("state", c.getState());
            container.put("status", c.getStatus());
            containers.add(container);
        });

        ctx.json(containers);
    }
}
