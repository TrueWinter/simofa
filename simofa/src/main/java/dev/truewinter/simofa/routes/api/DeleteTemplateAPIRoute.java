package dev.truewinter.simofa.routes.api;

import dev.truewinter.simofa.Template;
import dev.truewinter.simofa.routes.Route;
import io.javalin.http.Context;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class DeleteTemplateAPIRoute extends Route {
    @Override
    public void post(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);

        try {
            Optional<Template> template = getDatabase().getTemplatesDatabase().getTemplateById(id);
            if (template.isPresent()) {
                getDatabase().getTemplatesDatabase().deleteTemplate(id);
                ctx.json(resp);
            } else {
                resp.put("success", false);
                ctx.status(404).json(resp);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            resp.put("success", false);
            ctx.status(500).json(resp);
        }
    }
}
