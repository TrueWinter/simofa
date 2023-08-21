package dev.truewinter.simofa.routes.api;

import dev.truewinter.simofa.Simofa;
import dev.truewinter.simofa.routes.Route;
import io.javalin.http.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DockerImagesAPIRoute extends Route {
    @Override
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
