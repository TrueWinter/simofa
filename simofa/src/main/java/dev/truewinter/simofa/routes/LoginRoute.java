package dev.truewinter.simofa.routes;

import dev.truewinter.simofa.LoginCookie;
import dev.truewinter.simofa.common.Util;
import io.javalin.http.Context;
import io.javalin.http.Cookie;

import java.sql.SQLException;

public class LoginRoute extends Route {
    public void get(Context ctx) {
        render(ctx, "login");
    }

    public void post(Context ctx) {
        String username = ctx.formParam("username");
        String password = ctx.formParam("password");

        if (Util.isBlank(username) || Util.isBlank(password)) {
            renderError(ctx, "login", "Username and password required");
            return;
        }

        try {
            getDatabase().getAccountDatabase().getAccountIfPasswordIsCorrect(username, password).ifPresentOrElse(
                    account -> {
                        try {
                            LoginCookie loginCookie = new LoginCookie(account.getId(), Util.generateRandomString(32));
                            Cookie cookie = new Cookie("simofa", loginCookie.getJWT());
                            cookie.setMaxAge(LoginCookie.EXPIRES_IN);
                            cookie.setHttpOnly(true);
                            ctx.cookie(cookie);

                            String redirectTo = ctx.queryParam("redirectTo");
                            if (Util.isBlank(redirectTo)) {
                                redirectTo = "/websites";
                            }

                            ctx.redirect(redirectTo);
                        } catch (Exception e) {
                            ctx.status(500).result("Unable to set login cookie");
                            e.printStackTrace();
                        }
                    }, () -> {
                        renderError(ctx, "login", "Invalid username or password");
                    }
            );
        } catch (SQLException e) {
            renderError(ctx, "login", "An error occurred");
            e.printStackTrace();
        }
    }
}
