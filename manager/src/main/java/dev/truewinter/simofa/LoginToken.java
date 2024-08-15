package dev.truewinter.simofa;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.time.Instant;

public class LoginToken {
    public static final int EXPIRES_IN = 24 * 60 * 60;
    private final String userId;

    public LoginToken(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public String getJWT() {
        Algorithm algorithm = Algorithm.HMAC256(Simofa.getSecret());
        return JWT.create()
                .withExpiresAt(Instant.ofEpochSecond((long) (System.currentTimeMillis() / 1000.0) + EXPIRES_IN))
                .withClaim("user_id", userId)
                .sign(algorithm);
    }

    public static LoginToken jwtToLoginToken(String jwtString) throws JWTVerificationException {
        Algorithm algorithm = Algorithm.HMAC256(Simofa.getSecret());
        JWTVerifier verifier = JWT.require(algorithm).acceptLeeway(10).build();
        DecodedJWT jwt = verifier.verify(jwtString);
        String userId = jwt.getClaim("user_id").asString();

        return new LoginToken(userId);
    }
}
