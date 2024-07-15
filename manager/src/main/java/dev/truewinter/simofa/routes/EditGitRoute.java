package dev.truewinter.simofa.routes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import dev.truewinter.simofa.api.GitCredential;
import dev.truewinter.simofa.RouteLoader;
import dev.truewinter.simofa.formvalidators.AddEditGitValidator;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;

import java.util.HashMap;
import java.util.Optional;

@SuppressWarnings("unused")
@RouteLoader.RouteClass()
public class EditGitRoute extends Route {
    private HashMap<String, Object> getGitCredentialDataForModel(GitCredential g) throws JsonProcessingException {
        HashMap<String, Object> containerMap = new HashMap<>();
        ObjectMapper objectMapper = JsonMapper.builder()
                .configure(MapperFeature.USE_ANNOTATIONS, false)
                .build();
        String gitJson = objectMapper.writeValueAsString(g);
        @SuppressWarnings("unchecked")
        HashMap<Object, Object> gitData = objectMapper.readValue(gitJson, HashMap.class);

        containerMap.put("git", gitData);

        return containerMap;
    }

    @RouteLoader.RouteInfo(
            url = "/git/{id}/edit"
    )
    public void get(Context ctx) {
        String id = ctx.pathParam("id");

        try {
            Optional<GitCredential> gitCredential = getDatabase().getGitDatabase().getGitCredential(id);
            if (gitCredential.isEmpty()) {
                ctx.status(404).result("Git credential not found");
            } else {
                render(ctx, "git/edit", getGitCredentialDataForModel(gitCredential.get()));
            }
        } catch (Exception e) {
            ctx.status(500).result("An error occurred");
        }
    }

    @RouteLoader.RouteInfo(
            url = "/git/{id}/edit",
            method = HandlerType.POST
    )
    public void post(Context ctx) {
        String id = ctx.pathParam("id");
        String username = ctx.formParam("username");
        String password = ctx.formParam("password");

        try {
            Optional<GitCredential> gitCredential = getDatabase().getGitDatabase().getGitCredential(id);
            if (gitCredential.isEmpty()) {
                ctx.status(404).result("Git credential not found");
            } else {
                Optional<String> error = new AddEditGitValidator().hasError(ctx);
                if (error.isPresent()) {
                    renderError(ctx, "git/add", error.get(), getGitCredentialDataForModel(gitCredential.get()));
                    return;
                }

                getDatabase().getGitDatabase().editGitCredential(new GitCredential(
                        id, username, password
                ));

                renderSuccess(ctx, "git/edit", "Git credential updated", getGitCredentialDataForModel(getDatabase().getGitDatabase().getGitCredential(id).get()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            ctx.status(500).result("An error occurred");
        }
    }
}
