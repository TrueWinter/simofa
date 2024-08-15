package dev.truewinter.simofa.routes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import dev.truewinter.simofa.Account;
import dev.truewinter.simofa.RouteLoader;
import dev.truewinter.simofa.Simofa;
import dev.truewinter.simofa.common.Util;
import dev.truewinter.simofa.routes.Route;
import io.javalin.http.*;

import java.sql.SQLException;
import java.util.*;

@SuppressWarnings("unused")
@RouteLoader.RouteClass()
public class AccountsRoute extends Route {
    @RouteLoader.RouteInfo(
            url = "/api/accounts"
    )
    public void getAllAccounts(Context ctx) {
        try {
            ctx.json(getDatabase().getAccountDatabase().getAccounts());
        } catch (SQLException e) {
            Simofa.getLogger().error("Failed to get accounts", e);
            throw new InternalServerErrorResponse("Failed to get accounts");
        }
    }

    @RouteLoader.RouteInfo(
            url = "/api/accounts/{id}"
    )
    public void getOneAccount(Context ctx) {
        String id = ctx.pathParam("id");

        try {
            getDatabase().getAccountDatabase().getAccountById(id).ifPresentOrElse(ctx::json, () -> {
                throw new NotFoundResponse("Account not found");
            });
        } catch (SQLException e) {
            Simofa.getLogger().error("Failed to get account", e);
            throw new InternalServerErrorResponse("Failed to get account");
        }
    }

    @RouteLoader.RouteInfo(
            url = "/api/accounts",
            method = HandlerType.POST
    )
    public void addAccount(Context ctx) throws JsonProcessingException {
        JsonNode json = toJson(ctx);
        if (!(json.hasNonNull("username") && json.hasNonNull("password") &&
                json.hasNonNull("confirmPassword"))) {
            throw new BadRequestResponse("All fields are required");
        }

        String username = json.get("username").asText();
        String password = json.get("password").asText();
        String confirmPassword = json.get("confirmPassword").asText();

        if (!password.equals(confirmPassword)) {
            throw new BadRequestResponse("Password and password confirmation must match");
        }

        try {
            String id = getDatabase().getAccountDatabase().addAccount(username, password);

            HashMap<String, String> resp = new HashMap<>();
            resp.put("id", id);
            ctx.json(resp);
        } catch (SQLException e) {
            Simofa.getLogger().error("Failed to add account", e);
            throw new InternalServerErrorResponse("Failed to add account");
        }
    }

    @RouteLoader.RouteInfo(
            url = "/api/accounts/{id}",
            method = HandlerType.PUT
    )
    public void editAccount(Context ctx) throws JsonProcessingException {
        String id = ctx.pathParam("id");
        JsonNode jsonNode = toJson(ctx);

        if (!jsonNode.hasNonNull("username")) {
            throw new BadRequestResponse("Username is required");
        }

        String username = jsonNode.get("username").asText();
        String password;
        String confirmPassword;

        if (jsonNode.hasNonNull("password") || jsonNode.hasNonNull("confirmPassword")) {
            if (!(jsonNode.hasNonNull("password") && jsonNode.hasNonNull("confirmPassword"))) {
                throw new BadRequestResponse("If password or password confirmation is present, the other one must also be present");
            }

            password = jsonNode.get("password").asText();
            confirmPassword = jsonNode.get("confirmPassword").asText();

            if (Util.isBlank(password)) {
                throw new BadRequestResponse("Password cannot be blank");
            }

            if (!password.equals(confirmPassword)) {
                throw new BadRequestResponse("Password and password confirmation must match");
            }
        } else {
            password = null;
        }

        try {
            Optional<Account> a = getDatabase().getAccountDatabase().getAccountById(id);
            if (a.isPresent()) {
                Account editedAccount;

                if (password == null) {
                    editedAccount = new Account(id, username, a.get().getPasswordHash());
                } else {
                    editedAccount = new Account(id, username, Account.createHash(password));
                }

                getDatabase().getAccountDatabase().editAccount(editedAccount);
            } else {
                throw new NotFoundResponse("Account not found");
            }
        } catch (SQLException e) {
            Simofa.getLogger().error("Failed to edit account", e);
            throw new InternalServerErrorResponse("Failed to edit account");
        }
    }

    @RouteLoader.RouteInfo(
            url = "/api/accounts/{id}",
            method = HandlerType.DELETE
    )
    public void deleteAccount(Context ctx) {
        String id = ctx.pathParam("id");

        try {
            getDatabase().getAccountDatabase().deleteAccount(id);
        } catch (SQLException e) {
            Simofa.getLogger().error("Failed to delete account", e);
            throw new InternalServerErrorResponse("Failed to delete account");
        }
    }
}
