package dev.truewinter.simofa.routes;

import dev.truewinter.simofa.RouteLoader;
import dev.truewinter.simofa.Simofa;
import dev.truewinter.simofa.api.Website;
import dev.truewinter.simofa.common.Util;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;

import java.util.Optional;

@SuppressWarnings("unused")
@RouteLoader.RouteClass()
public class PullWebsiteRoute extends Route {
    @RouteLoader.RouteInfo(
            url = "/websites/{id}/pull",
            method = HandlerType.POST
    )
    public void post(Context ctx) {
        String id = ctx.pathParam("id");
        String gitUrl = ctx.formParam("repository");
        String gitBranch = ctx.formParam("branch");
        String noCache = ctx.formParam("noCache");

        if (Util.isBlank(gitUrl) || Util.isBlank(gitBranch)) {
            renderError(ctx, "error", "Required fields missing from request");
            return;
        }

        try {
            Optional<Website> website = getDatabase().getWebsiteDatabase().getWebsiteById(id);
            if (website.isPresent()) {
                String commitMsg = "<manual build>";
                if (noCache != null) {
                    commitMsg = "[no cache] " + commitMsg;
                }

                Simofa.getBuildQueueManager().getBuildQueue().queue(website.get(), commitMsg);
                redirect(ctx,"/websites/" + id + "/logs");
            } else {
                renderError(ctx, "error", "Website not found");
            }
        } catch (Exception e) {
            e.printStackTrace();
            renderError(ctx, "error", "Failed to pull website");
        }
    }
}
