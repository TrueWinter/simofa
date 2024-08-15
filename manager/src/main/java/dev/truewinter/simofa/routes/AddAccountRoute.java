package dev.truewinter.simofa.routes;

import dev.truewinter.simofa.RouteLoader;
import dev.truewinter.simofa.formvalidators.AddAccountValidator;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;

import java.sql.SQLException;
import java.util.Optional;

@SuppressWarnings("unused")
@RouteLoader.RouteClass
public class AddAccountRoute extends Route {
    @RouteLoader.RouteInfo(
            url = "/accounts/add"
    )
    public void get(Context ctx) {
        render(ctx, "accounts/add");
    }

    @RouteLoader.RouteInfo(
            url = "/accounts/add",
            method = HandlerType.POST
    )
    public void post(Context ctx) {
        String username = ctx.formParam("username");
        String password = ctx.formParam("password");

        Optional<String> error = new AddAccountValidator().hasError(ctx);
        if (error.isPresent()) {
            renderError(ctx, "accounts/add", error.get());
            return;
        }

        try {
            if (getDatabase().getAccountDatabase().getAccountByUsername(username).isPresent()) {
                renderError(ctx, "accounts/add", "User with that username already exists");
                return;
            }

            getDatabase().getAccountDatabase().addAccount(username, password);
            redirect(ctx, "/accounts");
        } catch (SQLException e) {
            e.printStackTrace();
            renderError(ctx, "accounts/add", "Failed to add account");
        }
    }
}
