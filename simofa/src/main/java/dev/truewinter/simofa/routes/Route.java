package dev.truewinter.simofa.routes;

import dev.truewinter.simofa.common.Util;
import dev.truewinter.simofa.database.Database;
import dev.truewinter.simofa.LoginCookie;
import dev.truewinter.simofa.config.Config;
import io.javalin.http.Context;
import org.apache.hc.core5.net.URIBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

abstract public class Route {
    private static String WEB_ROOT = "web/html/";
    private static final String EXTENSION = ".peb";
    private static Database database;
    private static Config config;
    private static Map<String, String> assetManifest;

    /*public void get(Context ctx) {}
    public void post(Context ctx) {}*/

    public static boolean verifyLogin(Context ctx) {
        if (!isLoggedIn(ctx)) {
            if (ctx.path().startsWith("/api")) {
                HashMap<String, Object> json = new HashMap<>();
                json.put("success", false);
                json.put("error", "Unauthorized");
                ctx.status(403).json(json);
            } else {
                if (isJwtQueryAuth(ctx)) {
                    renderError(ctx, "error", "You do not have access to that page, or your token is expired.");
                } else {
                    String redirectTo = ctx.path();
                    ctx.redirect("/login?redirectTo=" + URLEncoder.encode(redirectTo, StandardCharsets.UTF_8));
                }
            }

            return false;
        }

        return true;
    }

    public static boolean verifyCSRF(Context ctx, String errorPage) {
        String csrf = ctx.formParam("csrf");

        Optional<LoginCookie> loginCookie = getLoginCookie(ctx);

        if (csrf == null || loginCookie.isEmpty() || !Util.secureCompare(csrf, loginCookie.get().getCsrf())) {
            ctx.status(400);
            renderError(ctx, errorPage, "CSRF token is invalid");
            return false;
        }

        return true;
    }

    private static void addDefaultModelValues(Context ctx, String view, HashMap<String, Object> model) {
        Optional<LoginCookie> cookie = getLoginCookie(ctx);
        if (cookie.isPresent()) {
            model.put("user_id", String.valueOf(cookie.get().getUserId()));
            model.put("csrf", cookie.get().getCsrf());
        }

        if (config.isDevMode()) {
            model.put("dev", "true");
        }

        model.put("assets", assetManifest);
        model.put("view", view);
        model.put("web_root", WEB_ROOT);

        try {
            model.put("SimofaVersion", Util.getVersion());
        } catch (IOException e) {
            e.printStackTrace();
            model.put("SimofaVersion", "x.x.x");
        }
    }

    public static final void render(Context ctx, String path) {
        render(ctx, path, new HashMap<>());
    }

    public static final void render(Context ctx, String path, HashMap<String, Object> model) {
        addDefaultModelValues(ctx, path, model);
        ctx.render(getPath(path), model);
    }

    public static final void renderError(Context ctx, String path, String error) {
        renderError(ctx, path, error, new HashMap<>());
    }

    public static final void renderError(Context ctx, String path, String error, HashMap<String, Object> model) {
        addDefaultModelValues(ctx, path, model);
        model.put("error", error);
        ctx.render(getPath(path), model);
    }

    public static final void renderSuccess(Context ctx, String path, String success) {
        renderSuccess(ctx, path, success, new HashMap<>());
    }

    public static final void renderSuccess(Context ctx, String path, String success, HashMap<String, Object> model) {
        addDefaultModelValues(ctx, path, model);
        model.put("success", success);
        ctx.render(getPath(path), model);
    }

    public static void redirect(Context ctx, String path) {
        String jwtQuery = ctx.queryParam("jwt");
        if (Util.isBlank(jwtQuery)) {
            ctx.redirect(path);
            return;
        }

        try {
            // Not really any good way to work on relative URLs other than this
            URL baseUrl = new URL("http://localhost:8808");
            URL url = new URL(baseUrl, path);
            URI uri = new URIBuilder(url.toString())
                    .addParameter("jwt", jwtQuery)
                    .build();

            ctx.redirect(uri.toString().replace(baseUrl.toString(), ""));
        } catch (Exception e) {
            e.printStackTrace();
            ctx.redirect(path);
        }
    }

    public static void setDatabaseInstance(Database database) {
        Route.database = database;
    }

    public static Database getDatabase() {
        return database;
    }

    public static void setConfig(Config config) {
        Route.config = config;
        if (config.isDevMode()) {
            WEB_ROOT = "simofa/src/main/resources/" + WEB_ROOT;
        }
    }

    public static void setAssetManifest(Map<String, String> assetManifest) {
        Route.assetManifest = assetManifest;
    }

    public static String getPath(String file) {
        return WEB_ROOT + file + EXTENSION;
    }

    public static boolean isLoggedIn(Context ctx) {
        return getLoginCookie(ctx).isPresent();
    }

    public static boolean isJwtQueryAuth(Context ctx) {
        String jwtQuery = ctx.queryParam("jwt");
        return !Util.isBlank(jwtQuery);
    }

    protected static Optional<LoginCookie> getLoginCookie(Context ctx) {
        String cookie = ctx.cookie("simofa");
        String jwtQuery = ctx.queryParam("jwt");

        try {
             if (!Util.isBlank(jwtQuery)) {
                LoginCookie loginCookie = LoginCookie.jwtToLoginCookie(jwtQuery, true);
                String path = ctx.path() + "?" + ctx.queryString();

                for (String route : loginCookie.getRoutes()) {
                    if (path.startsWith(route)) {
                        return Optional.of(loginCookie);
                    }
                }

                return Optional.empty();
             } else if (!Util.isBlank(cookie)) {
                 return Optional.of(LoginCookie.jwtToLoginCookie(cookie));
             }

            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
