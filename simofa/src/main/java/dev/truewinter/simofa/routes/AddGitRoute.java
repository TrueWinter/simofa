package dev.truewinter.simofa.routes;

import dev.truewinter.simofa.api.GitCredential;
import dev.truewinter.simofa.RouteLoader;
import dev.truewinter.simofa.common.Util;
import dev.truewinter.simofa.formvalidators.AddEditGitValidator;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;

import java.util.Optional;

@SuppressWarnings("unused")
@RouteLoader.RouteClass()
public class AddGitRoute extends Route {
    @RouteLoader.RouteInfo(
            url = "/git/add"
    )
    public void get(Context ctx) {
        render(ctx, "git/add");
    }

    @RouteLoader.RouteInfo(
            url = "/git/add",
            method = HandlerType.POST
    )
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
                    Util.createv7UUID().toString(), username, password
            ));

            redirect(ctx,"/git");
        } catch (Exception e) {
            renderError(ctx, "git/add", "An error occurred");
        }
    }
}
