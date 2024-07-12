package dev.truewinter.simofa.routes;

import dev.truewinter.simofa.RouteLoader;
import dev.truewinter.simofa.api.Website;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;

import java.util.Optional;

@SuppressWarnings("unused")
@RouteLoader.RouteClass()
public class DeleteWebsiteRoute extends Route {
    @RouteLoader.RouteInfo(
            url = "/websites/{id}/delete",
            method = HandlerType.POST
    )
    public void post(Context ctx) {
        String id = ctx.pathParam("id");

        try {
            Optional<Website> w = getDatabase().getWebsiteDatabase().getWebsiteById(id);
            if (w.isPresent()) {
                getDatabase().getWebsiteDatabase().deleteWebsite(id);
                redirect(ctx,"/websites");
            } else {
                ctx.status(404).result("Website does not exist");
            }
        } catch (Exception e) {
            ctx.status(500).result("Failed to delete website: " + e.getMessage());
        }
    }
}
