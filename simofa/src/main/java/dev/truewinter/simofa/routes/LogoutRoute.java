package dev.truewinter.simofa.routes;

import dev.truewinter.simofa.RouteLoader;
import io.javalin.http.Context;

@SuppressWarnings("unused")
@RouteLoader.RouteClass(
        // TODO: Remove
        //verifyCsrf = false
)
public class LogoutRoute extends Route {
    @RouteLoader.RouteInfo(
            url = "/logout"
    )
    public void get(Context ctx) {
        ctx.removeCookie("simofa");
        ctx.redirect("/login");
    }
}
