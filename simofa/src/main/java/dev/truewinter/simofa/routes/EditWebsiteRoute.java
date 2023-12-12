package dev.truewinter.simofa.routes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.truewinter.simofa.GitCredential;
import dev.truewinter.simofa.RouteLoader;
import dev.truewinter.simofa.Website;
import dev.truewinter.simofa.formvalidators.AddEditWebsiteValidator;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;

import java.util.HashMap;
import java.util.Optional;

@SuppressWarnings("unused")
@RouteLoader.RouteClass()
public class EditWebsiteRoute extends Route {
    private HashMap<String, Object> getWebsiteDataForModel(Website w) throws JsonProcessingException {
        // This configuration makes it easier to get the
        // relevant data in the Pebble template.
        HashMap<String, Object> containerMap = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        String websiteJson = objectMapper.writeValueAsString(w);
        @SuppressWarnings("unchecked")
        HashMap<Object, Object> websiteData = objectMapper.readValue(websiteJson, HashMap.class);

        containerMap.put("website", websiteData);

        return containerMap;
    }

    @RouteLoader.RouteInfo(
            url = "/websites/{id}/edit"
    )
    public void get(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        try {
            Optional<Website> website = getDatabase().getWebsiteDatabase().getWebsiteById(id);
            website.ifPresentOrElse(w -> {
                try {
                    render(ctx, "websites/edit", getWebsiteDataForModel(w));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                    ctx.result("An error occurred");
                }
            }, () -> {
                ctx.result("Website does not exist");
            });
        } catch (Exception e) {
            e.printStackTrace();
            ctx.result("An error occurred");
        }
    }

    @RouteLoader.RouteInfo(
            url = "/websites/{id}/edit",
            method = HandlerType.POST
    )
    public void post(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        try {
            Optional<Website> w = getDatabase().getWebsiteDatabase().getWebsiteById(id);
            if (w.isEmpty()) {
                ctx.result("Website does not exist");
                return;
            }

            String name = ctx.formParam("name");
            String dockerImage = ctx.formParam("docker_image");
            String gitUrl = ctx.formParam("git_url");
            String gitBranch = ctx.formParam("git_branch");
            String gitCredential = ctx.formParam("git_credential");
            String buildCommand = ctx.formParam("build_command");
            String deploymentCommand = ctx.formParam("deployment_command");
            String deploymentFailedCommand = ctx.formParam("deployment_failed_command");
            String deploymentServer = ctx.formParam("deployment_server");
            String deployToken = ctx.formParam("deploy_token");

            Optional<String> error = new AddEditWebsiteValidator().hasError(ctx);

            if (error.isPresent()) {
                renderError(ctx, "websites/edit", error.get(), getWebsiteDataForModel(w.get()));
                return;
            }

            // It may be null above, but the validator ensures that it isn't
            @SuppressWarnings("ConstantConditions")
            int memory = Integer.parseInt(ctx.formParam("memory"));
            @SuppressWarnings("ConstantConditions")
            double cpu = Double.parseDouble(ctx.formParam("cpu"));

            GitCredential gitCredentialRef = null;
            //noinspection ConstantConditions
            if (!gitCredential.equals("anonymous")) {
                try {
                    Optional<GitCredential> gitCredential1 = getDatabase().getGitDatabase().getGitCredential(Integer.parseInt(gitCredential));
                    if (gitCredential1.isPresent()) {
                        gitCredentialRef = gitCredential1.get();
                    } else {
                        throw new Exception("Git credential does not exist");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    renderError(ctx, "websites/add", "Failed to add website");
                }
            }

            @SuppressWarnings("ConstantConditions")
            Website website = new Website(
                    id,
                    name,
                    dockerImage,
                    memory,
                    cpu,
                    gitUrl,
                    gitBranch,
                    gitCredentialRef,
                    buildCommand,
                    deploymentCommand,
                    deploymentFailedCommand,
                    Integer.parseInt(deploymentServer),
                    deployToken
            );

            getDatabase().getWebsiteDatabase().editWebsite(website);
            try {
                renderSuccess(ctx, "websites/edit", "Edited website", getWebsiteDataForModel(getDatabase().getWebsiteDatabase().getWebsiteById(id).get()));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                ctx.result("Website updated, but page rendering failed");
            }
        } catch(Exception e) {
            e.printStackTrace();
            ctx.result("Failed to edit website");
        }
    }
}
