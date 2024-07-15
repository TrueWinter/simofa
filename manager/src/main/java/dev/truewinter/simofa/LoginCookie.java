package dev.truewinter.simofa;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;

public class LoginCookie {
    public static final int EXPIRES_IN = 24 * 60 * 60;
    private final String userId;
    private final String csrf;
    private final HashSet<String> routes;

    public LoginCookie(String userId, String csrf) {
        this.userId = userId;
        this.csrf = csrf;
        this.routes = new HashSet<>();
        this.routes.add("/");
    }

    public LoginCookie(String userId, String csrf, HashSet<String> routes) {
        this.userId = userId;
        this.csrf = csrf;
        this.routes = routes;
    }

    // User ID (or null if authenticated with JWT query parameter)
    public String getUserId() {
        return userId;
    }

    public String getCsrf() {
        return csrf;
    }

    public HashSet<String> getRoutes() {
        return routes;
    }

    public String getJWT() {
        Algorithm algorithm = Algorithm.HMAC256(Simofa.getSecret());
        return JWT.create()
                .withExpiresAt(Instant.ofEpochSecond((long) (System.currentTimeMillis() / 1000.0) + EXPIRES_IN))
                .withClaim("user_id", userId)
                .withClaim("csrf", csrf)
                .withClaim("routes", this.routes.stream().toList())
                .sign(algorithm);
    }

    public static LoginCookie jwtToLoginCookie(String jwtString) throws JWTVerificationException {
        return jwtToLoginCookie(jwtString, false);
    }

    public static LoginCookie jwtToLoginCookie(String jwtString, boolean jwtQuery) throws JWTVerificationException {
        Algorithm algorithm = Algorithm.HMAC256(Simofa.getSecret());
        JWTVerifier verifier = JWT.require(algorithm).acceptLeeway(10).build();
        DecodedJWT jwt = verifier.verify(jwtString);
        String userId = null;
        if (!jwtQuery) {
            userId = jwt.getClaim("user_id").asString();
        }
        String csrf = jwt.getClaim("csrf").asString();
        HashSet<String> routes = new HashSet<>(jwt.getClaim("routes").asList(String.class));

        return new LoginCookie(userId, csrf, routes);
    }
}
