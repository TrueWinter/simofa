package dev.truewinter.simofa.routes;

import io.javalin.http.Context;

public class AccountsRoute extends Route {
    @Override
    public void get(Context ctx) {
        render(ctx, "accounts/list");
    }
}
