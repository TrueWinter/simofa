package dev.truewinter.simofa.routes;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.truewinter.simofa.RouteLoader;
import dev.truewinter.simofa.api.Website;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.Optional;

@SuppressWarnings("unused")
@RouteLoader.RouteClass()
public class LogsRoute extends Route {
    @RouteLoader.RouteInfo(
            url = "/websites/{id}/logs"
    )
    public void get(Context ctx) {
        String id = ctx.pathParam("id");
        try {
            Optional<Website> website = getDatabase().getWebsiteDatabase().getWebsiteById(id);

            if (website.isPresent()) {
                HashMap<String, Object> model = new HashMap<>();
                ObjectMapper objectMapper = new ObjectMapper();
                String websiteJson = objectMapper.writeValueAsString(website.get());
                @SuppressWarnings("unchecked")
                HashMap<Object, Object> websiteData = objectMapper.readValue(websiteJson, HashMap.class);

                model.put("website", websiteData);
                render(ctx, "logs/app", model);
            } else {
                renderError(ctx, "error", "Website not found");
            }
        } catch (Exception e) {
            e.printStackTrace();
            renderError(ctx, "error", "Failed to get website data");
        }
    }
}
