package dev.truewinter.simofa.routes;

import dev.truewinter.simofa.GitCredential;
import dev.truewinter.simofa.formvalidators.AddEditGitValidator;
import io.javalin.http.Context;

import java.util.Optional;

public class AddGitRoute extends Route {
    @Override
    public void get(Context ctx) {
        render(ctx, "git/add");
    }

    @Override
    public void post(Context ctx) {
        String username = ctx.formParam("username");
        String password = ctx.formParam("password");

        Optional<String> error = new AddEditGitValidator().hasError(ctx);
        if (error.isPresent()) {
            renderError(ctx, "git/add", error.get());
            return;
        }

        try {
            getDatabase().getGitDatabase().addGitCredential(new GitCredential(
                    0, username, password
            ));

            redirect(ctx,"/git");
        } catch (Exception e) {
            renderError(ctx, "git/add", "An error occurred");
        }
    }
}
