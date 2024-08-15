package dev.truewinter.simofa;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.time.Instant;

public class WsToken {
    private final String roomId;
    private final int expiry;

    public WsToken(String roomId) {
        this.roomId = roomId;
        /*
            WebSocket tokens are only used for the connection.
            Once connected, the WebSocket connection stays
            active until the client disconnects.
         */
        this.expiry = 5 * 60;
    }

    public WsToken(String roomId, int expiry) {
        this.roomId = roomId;
        this.expiry = expiry;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getJWT() {
        Algorithm algorithm = Algorithm.HMAC256(Simofa.getSecret());
        return JWT.create()
                .withExpiresAt(Instant.ofEpochSecond((long) (System.currentTimeMillis() / 1000.0) + expiry))
                .withClaim("room_id", roomId)
                .sign(algorithm);
    }

    public static WsToken jwtToWsToken(String jwtString) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(Simofa.getSecret());
            JWTVerifier verifier = JWT.require(algorithm).acceptLeeway(10).build();
            DecodedJWT jwt = verifier.verify(jwtString);
            String roomId1 = jwt.getClaim("room_id").asString();

            return new WsToken(roomId1);
        } catch (Exception ignored) {
            return null;
        }
    }
}
