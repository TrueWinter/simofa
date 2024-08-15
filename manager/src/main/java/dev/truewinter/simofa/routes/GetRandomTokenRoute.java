package dev.truewinter.simofa.routes;

import dev.truewinter.simofa.RouteLoader;
import dev.truewinter.simofa.common.Util;
import dev.truewinter.simofa.routes.Route;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;

import java.util.HashMap;

@SuppressWarnings("unused")
@RouteLoader.RouteClass
public class GetRandomTokenRoute extends Route {
    @RouteLoader.RouteInfo(
            url = "/api/random"
    )
    public void get(Context ctx) {
        String length = ctx.queryParam("length");
        if (Util.isBlank(length)) throw new BadRequestResponse("Length is required");

        int lengthInt = Integer.parseInt(length);
        if (lengthInt < 8 || lengthInt > 64) throw new BadRequestResponse("Length must be between 8 and 64");

        HashMap<String, String> resp = new HashMap<>();
        resp.put("random", Util.generateRandomString(lengthInt));
        ctx.json(resp);
    }
}
