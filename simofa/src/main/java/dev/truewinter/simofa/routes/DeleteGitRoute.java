package dev.truewinter.simofa.routes;

import dev.truewinter.simofa.api.GitCredential;
import dev.truewinter.simofa.RouteLoader;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;

import java.util.Optional;

@SuppressWarnings("unused")
@RouteLoader.RouteClass()
public class DeleteGitRoute extends Route {
    @RouteLoader.RouteInfo(
            url = "/git/{id}/delete",
            method = HandlerType.POST
    )
    public void post(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));

        try {
            Optional<GitCredential> gitCredential = getDatabase().getGitDatabase().getGitCredential(id);
            if (gitCredential.isEmpty()) {
                ctx.status(404).result("Git credential not found");
            } else {
                getDatabase().getGitDatabase().deleteGitCredential(id);
                redirect(ctx,"/git");
            }
        } catch (Exception e) {
            ctx.status(500).result("An error occurred");
        }
    }
}
