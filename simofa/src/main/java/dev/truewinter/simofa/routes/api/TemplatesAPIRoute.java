package dev.truewinter.simofa.routes.api;

import dev.truewinter.simofa.Template;
import dev.truewinter.simofa.routes.Route;
import io.javalin.http.Context;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TemplatesAPIRoute extends Route {
    @Override
    public void get(Context ctx) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("templates", new ArrayList<>());

        try {
            List<Template> templates = getDatabase().getTemplatesDatabase().getTemplates();
            resp.put("templates", templates);
        } catch (SQLException e) {
            e.printStackTrace();
            resp.put("success", false);
        }

        ctx.json(resp);
    }
}
