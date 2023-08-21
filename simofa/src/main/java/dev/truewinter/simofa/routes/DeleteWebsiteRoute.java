package dev.truewinter.simofa.routes;

import dev.truewinter.simofa.Website;
import io.javalin.http.Context;

import java.util.Optional;

public class DeleteWebsiteRoute extends Route {
    @Override
    public void post(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));

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
