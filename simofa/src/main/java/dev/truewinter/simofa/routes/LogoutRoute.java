package dev.truewinter.simofa.routes;

import io.javalin.http.Context;

public class LogoutRoute extends Route {
    @Override
    public void get(Context ctx) {
        ctx.removeCookie("simofa");
        ctx.redirect("/login");
    }
}
