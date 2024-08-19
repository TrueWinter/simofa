package dev.truewinter.simofa.routes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.truewinter.simofa.api.Website;
import dev.truewinter.simofa.common.Util;
import dev.truewinter.simofa.config.Config;
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
    private static Config config;
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

    public static void init(Config config, Database database) {
        Route.config = config;
        Route.database = database;
    }

    protected static Database getDatabase() {
        return database;
    }

    protected static Config getConfig() {
        return config;
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
