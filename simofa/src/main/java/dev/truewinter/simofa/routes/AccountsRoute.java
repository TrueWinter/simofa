package dev.truewinter.simofa.routes;

import dev.truewinter.simofa.RouteLoader;
import io.javalin.http.Context;

@SuppressWarnings("unused")
@RouteLoader.RouteClass()
public class AccountsRoute extends Route {
    @RouteLoader.RouteInfo(
            url = "/accounts"
    )
    public void get(Context ctx) {
        render(ctx, "accounts/list");
    }
}
