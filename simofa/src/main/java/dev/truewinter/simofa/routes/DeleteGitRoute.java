package dev.truewinter.simofa.routes;

import dev.truewinter.simofa.GitCredential;
import io.javalin.http.Context;

import java.util.Optional;

public class DeleteGitRoute extends Route {
    @Override
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
