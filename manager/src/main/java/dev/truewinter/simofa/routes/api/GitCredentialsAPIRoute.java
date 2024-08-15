package dev.truewinter.simofa.routes.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import dev.truewinter.simofa.Account;
import dev.truewinter.simofa.Simofa;
import dev.truewinter.simofa.RouteLoader;
import dev.truewinter.simofa.api.GitCredential;
import dev.truewinter.simofa.common.Util;
import dev.truewinter.simofa.routes.Route;
import io.javalin.http.*;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Optional;

@SuppressWarnings("unused")
@RouteLoader.RouteClass()
public class GitCredentialsAPIRoute extends Route {
    @RouteLoader.RouteInfo(
            url = "/api/git-credentials"
    )
    public void get(Context ctx) {
        try {
            ctx.json(getDatabase().getGitDatabase().getGitCredentials());
        } catch (SQLException e) {
            Simofa.getLogger().error("Failed to get Git credentials", e);
            throw new InternalServerErrorResponse("Failed to get Git credentials");
        }
    }

    @RouteLoader.RouteInfo(
            url = "/api/git-credentials/{id}"
    )
    public void getOneGitCredential(Context ctx) {
        String id = ctx.pathParam("id");

        try {
            getDatabase().getGitDatabase().getGitCredential(id).ifPresentOrElse(ctx::json, () -> {
                throw new NotFoundResponse("Git credential not found");
            });
        } catch (SQLException e) {
            Simofa.getLogger().error("Failed to get Git credentials", e);
            throw new InternalServerErrorResponse("Failed to get Git credentials");
        }
    }

    @RouteLoader.RouteInfo(
            url = "/api/git-credentials",
            method = HandlerType.POST
    )
    public void addGitCredential(Context ctx) throws JsonProcessingException {
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
            GitCredential gitCredential = new GitCredential(null, username, password);
            String id = getDatabase().getGitDatabase().addGitCredential(gitCredential);

            HashMap<String, String> resp = new HashMap<>();
            resp.put("id", id);
            ctx.json(resp);
        } catch (SQLException e) {
            Simofa.getLogger().error("Failed to add git credential", e);
            throw new InternalServerErrorResponse("Failed to add git credential");
        }
    }

    @RouteLoader.RouteInfo(
            url = "/api/git-credentials/{id}",
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
            Optional<GitCredential> g = getDatabase().getGitDatabase().getGitCredential(id);
            if (g.isPresent()) {
                GitCredential editedCredential;

                if (password == null) {
                    editedCredential = new GitCredential(id, username, g.get().getPassword());
                } else {
                    editedCredential = new GitCredential(id, username, password);
                }

                getDatabase().getGitDatabase().editGitCredential(editedCredential);
            } else {
                throw new NotFoundResponse("Git credential not found");
            }
        } catch (SQLException e) {
            Simofa.getLogger().error("Failed to edit git credential", e);
            throw new InternalServerErrorResponse("Failed to edit git credential");
        }
    }

    @RouteLoader.RouteInfo(
            url = "/api/git-credentials/{id}",
            method = HandlerType.DELETE
    )
    public void deleteAccount(Context ctx) {
        String id = ctx.pathParam("id");

        try {
            getDatabase().getGitDatabase().deleteGitCredential(id);
        } catch (SQLException e) {
            Simofa.getLogger().error("Failed to delete git credential", e);
            throw new InternalServerErrorResponse("Failed to delete git credential");
        }
    }
}
