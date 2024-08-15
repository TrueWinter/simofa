package dev.truewinter.simofa.routes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.truewinter.simofa.api.Website;
import dev.truewinter.simofa.common.Util;
import dev.truewinter.simofa.database.Database;
import dev.truewinter.simofa.LoginToken;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

abstract public class Route {
    private static String WEB_ROOT = "web/html/";
    private static final String EXTENSION = ".peb";
    private static Database database;

    public static boolean verifyLogin(Context ctx) {
        if (!isLoggedIn(ctx)) {
            if (ctx.path().startsWith("/api")) {
                throw new UnauthorizedResponse();
            } else {
                String redirectTo = ctx.path();
                ctx.redirect("/login?redirectTo=" + URLEncoder.encode(redirectTo, StandardCharsets.UTF_8));
            }

            return false;
        }

        return true;
    }

    public <T> T ctxToT(Context ctx, Class<T> t, String id) throws JsonProcessingException {
        ObjectNode json = toJson(ctx);
        json.put("id", id);
        return new ObjectMapper().treeToValue(json, t);
    }

    @Deprecated
    private static void addDefaultModelValues(Context ctx, String view, HashMap<String, Object> model) {
        Optional<LoginToken> cookie = getLoginCookie(ctx);
        if (cookie.isPresent()) {
            model.put("user_id", String.valueOf(cookie.get().getUserId()));
        }

        model.put("view", view);
        model.put("web_root", WEB_ROOT);

        try {
            model.put("SimofaVersion", Util.getVersion());
        } catch (IOException e) {
            e.printStackTrace();
            model.put("SimofaVersion", "x.x.x");
        }
    }

    @Deprecated
    public static final void render(Context ctx, String path) {
        render(ctx, path, new HashMap<>());
    }

    @Deprecated
    public static final void render(Context ctx, String path, HashMap<String, Object> model) {
        addDefaultModelValues(ctx, path, model);
        ctx.render(getPath(path), model);
    }

    @Deprecated
    public static final void renderError(Context ctx, String path, String error) {
        renderError(ctx, path, error, new HashMap<>());
    }

    @Deprecated
    public static final void renderError(Context ctx, String path, String error, HashMap<String, Object> model) {
        addDefaultModelValues(ctx, path, model);
        model.put("error", error);
        ctx.render(getPath(path), model);
    }

    @Deprecated
    public static final void renderSuccess(Context ctx, String path, String success) {
        renderSuccess(ctx, path, success, new HashMap<>());
    }

    @Deprecated
    public static final void renderSuccess(Context ctx, String path, String success, HashMap<String, Object> model) {
        addDefaultModelValues(ctx, path, model);
        model.put("success", success);
        ctx.render(getPath(path), model);
    }

    @Deprecated
    public static void redirect(Context ctx, String path) {
        ctx.redirect(path);
    }

    public static void setDatabaseInstance(Database database) {
        Route.database = database;
    }

    public static Database getDatabase() {
        return database;
    }

    public static String getPath(String file) {
        return WEB_ROOT + file + EXTENSION;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isLoggedIn(Context ctx) {
        return getLoginCookie(ctx).isPresent();
    }

    protected static Optional<LoginToken> getLoginCookie(Context ctx) {
        String token = ctx.header("Authorization");

        try {
             String jwt = Objects.requireNonNull(token).split(" ")[1];
             return Optional.of(LoginToken.jwtToLoginToken(jwt));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static ObjectNode toJson(Context ctx) throws JsonProcessingException {
        return (ObjectNode) new ObjectMapper().readTree(ctx.body());
    }
}
