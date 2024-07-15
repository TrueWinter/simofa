package dev.truewinter.simofa.routes.api;

import dev.truewinter.simofa.RouteLoader;
import dev.truewinter.simofa.api.Website;
import dev.truewinter.simofa.routes.Route;
import io.javalin.http.Context;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
@RouteLoader.RouteClass()
public class WebsitesAPIRoute extends Route {
    @RouteLoader.RouteInfo(
            url = "/api/websites"
    )
    public void get(Context ctx) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("websites", new ArrayList<>());

        try {
            List<Website> websites = getDatabase().getWebsiteDatabase().getWebsites();
            resp.put("websites", websites);
        } catch (SQLException e) {
            e.printStackTrace();
            resp.put("success", false);
        }

        ctx.json(resp);
    }
}
