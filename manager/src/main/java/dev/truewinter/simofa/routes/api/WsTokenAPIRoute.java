package dev.truewinter.simofa.routes.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import dev.truewinter.simofa.RouteLoader;
import dev.truewinter.simofa.WsToken;
import dev.truewinter.simofa.common.Util;
import dev.truewinter.simofa.routes.Route;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;

import java.util.HashMap;

@SuppressWarnings("unused")
@RouteLoader.RouteClass
public class WsTokenAPIRoute extends Route {
    @RouteLoader.RouteInfo(
            url = "/api/login/ws",
            method = HandlerType.POST
    )
    public void post(Context ctx) throws JsonProcessingException {
        JsonNode roomId = toJson(ctx).get("roomId");
        if (roomId == null) {
            throw new BadRequestResponse("Room ID is required");
        }

        String roomIdString = roomId.asText();
        if (Util.isBlank(roomIdString)) {
            throw new BadRequestResponse("Room ID is required");
        }

        WsToken wsToken = new WsToken(roomIdString);
        HashMap<String, String> resp = new HashMap<>();
        resp.put("token", wsToken.getJWT());
        ctx.json(resp);
    }
}
