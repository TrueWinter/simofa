package dev.truewinter.simofa.routes.api;

import dev.truewinter.simofa.RouteLoader;
import dev.truewinter.simofa.Simofa;
import dev.truewinter.simofa.routes.Route;
import io.javalin.http.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@SuppressWarnings("unused")
@RouteLoader.RouteClass()
public class DockerImagesAPIRoute extends Route {
    @RouteLoader.RouteInfo(
            url = "/api/builds/images"
    )
    public void get(Context ctx) {
        List<HashMap<String, String>> images = new ArrayList<>();
        Simofa.getDockerManager().getImages().forEach(i -> {
            HashMap<String, String> img = new HashMap<>();
            img.put("name", i.getName());
            img.put("size", i.getSize());
            images.add(img);
        });

        ctx.json(images);
    }
}
