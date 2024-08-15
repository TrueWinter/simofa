package dev.truewinter.simofa.routes.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.truewinter.simofa.RouteLoader;
import dev.truewinter.simofa.Simofa;
import dev.truewinter.simofa.Template;
import dev.truewinter.simofa.common.Util;
import dev.truewinter.simofa.routes.Route;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.http.InternalServerErrorResponse;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
@RouteLoader.RouteClass()
public class TemplatesAPIRoute extends Route {
    @RouteLoader.RouteInfo(
            url = "/api/templates"
    )
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

    @RouteLoader.RouteInfo(
            url = "/api/templates",
            method = HandlerType.POST
    )
    public void post(Context ctx) throws Exception {
        try {
            JsonNode json = toJson(ctx);
            Template template = new ObjectMapper().treeToValue(json, Template.class);
            String id = getDatabase().getTemplatesDatabase().addTemplate(template);

            HashMap<String, String> resp = new HashMap<>();
            resp.put("id", id);
            ctx.json(resp);
        } catch (Exception e) {
            throw new InternalServerErrorResponse("Failed to save template: " + e.getMessage());
        }
    }

    @RouteLoader.RouteInfo(
            url = "/api/templates/{id}",
            method = HandlerType.DELETE
    )
    public void delete(Context ctx) throws SQLException {
        String id = ctx.pathParam("id");
        getDatabase().getTemplatesDatabase().deleteTemplate(id);
    }
}
