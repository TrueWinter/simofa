package dev.truewinter.simofa.routes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import dev.truewinter.simofa.LoginToken;
import dev.truewinter.simofa.RouteLoader;
import dev.truewinter.simofa.Simofa;
import dev.truewinter.simofa.common.Util;
import io.javalin.http.*;

import java.sql.SQLException;
import java.util.HashMap;

@SuppressWarnings("unused")
@RouteLoader.RouteClass(
        verifyLogin = false
)
public class LoginRoute extends Route {
    @RouteLoader.RouteInfo(
            url = "/api/login",
            method = HandlerType.POST
    )
    public void post(Context ctx) throws JsonProcessingException {
        JsonNode data = toJson(ctx);
        String username = data.get("username").asText();
        String password = data.get("password").asText();

        if (Util.isBlank(username) || Util.isBlank(password)) {
            throw new InternalServerErrorResponse("Username and password required");
        }

        try {
            getDatabase().getAccountDatabase().getAccountIfPasswordIsCorrect(username, password).ifPresentOrElse(
                account -> {
                    LoginToken loginToken = new LoginToken(account.getId());
                    HashMap<String, String> successResp = new HashMap<>();
                    successResp.put("token", loginToken.getJWT());
                    ctx.json(successResp);
                }, () -> {
                    throw new UnauthorizedResponse("Invalid username or password");
                }
            );
        } catch (SQLException e) {
            Simofa.getLogger().error("Failed to validate login", e);
            throw new InternalServerErrorResponse();
        }
    }
}
