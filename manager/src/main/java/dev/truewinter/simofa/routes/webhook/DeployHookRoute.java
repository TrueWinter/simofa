package dev.truewinter.simofa.routes.webhook;

import dev.truewinter.simofa.RouteLoader;
import dev.truewinter.simofa.Simofa;
import dev.truewinter.simofa.common.Util;
import dev.truewinter.simofa.routes.Route;
import io.javalin.http.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

@SuppressWarnings("unused")
@RouteLoader.RouteClass(
        verifyLogin = false
)
public class DeployHookRoute extends Route {
    @RouteLoader.RouteInfo(
            url = "/public-api/deploy-hook",
            method = HandlerType.POST
    )
    public void post(Context ctx) throws SQLException {
        String website = ctx.queryParam("website");
        String token = ctx.queryParam("token");
        String commit = ctx.queryParam("commit");

        if (Util.isBlank(website, token)) {
            throw new BadRequestResponse("Website and token parameters are required");
        }

        getDatabase().getWebsiteDatabase().getWebsiteById(website).ifPresentOrElse(w -> {
            // noinspection DataFlowIssue
            if (!Util.secureCompare(w.getDeployToken(), token)) {
                throw new UnauthorizedResponse("Invalid deploy token");
            }

            String commitMsg = "<deploy hook>";
            if (!Util.isBlank(commit)) {
                // noinspection DataFlowIssue
                String msg = URLDecoder.decode(commit, StandardCharsets.UTF_8);
                if (msg != null) {
                    commitMsg = msg;
                }
            }

            Simofa.getBuildQueueManager().getBuildQueue().queue(w, commitMsg);
        }, () -> {
            throw new NotFoundResponse("Website not found");
        });
    }
}
