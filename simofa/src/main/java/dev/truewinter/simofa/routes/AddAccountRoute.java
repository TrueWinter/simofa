package dev.truewinter.simofa.routes;

import dev.truewinter.simofa.formvalidators.AddAccountValidator;
import io.javalin.http.Context;

import java.sql.SQLException;
import java.util.Optional;

public class AddAccountRoute extends Route {
    @Override
    public void get(Context ctx) {
        render(ctx, "accounts/add");
    }

    @Override
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
            renderSuccess(ctx, "accounts/add", "Added account");
        } catch (SQLException e) {
            e.printStackTrace();
            renderError(ctx, "accounts/add", "Failed to add account");
        }
    }
}
