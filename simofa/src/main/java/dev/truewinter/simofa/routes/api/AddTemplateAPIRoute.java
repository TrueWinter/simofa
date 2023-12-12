package dev.truewinter.simofa.routes.api;

import dev.truewinter.simofa.RouteLoader;
import dev.truewinter.simofa.Template;
import dev.truewinter.simofa.common.Util;
import dev.truewinter.simofa.routes.Route;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
@RouteLoader.RouteClass()
public class AddTemplateAPIRoute extends Route {
    @RouteLoader.RouteInfo(
            url = "/api/templates/add",
            method = HandlerType.POST
    )
    public void post(Context ctx) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);

        String name = ctx.formParam("name");
        String template = ctx.formParam("template");

        if (Util.isBlank(name) || Util.isBlank(template)) {
            resp.put("success", false);
            ctx.json(resp);
            return;
        }

        try {
            Template template1 = new Template(0, name, template);
            getDatabase().getTemplatesDatabase().addTemplate(template1);
        } catch (SQLException e) {
            e.printStackTrace();
            resp.put("success", false);
        }

        ctx.json(resp);
    }
}
