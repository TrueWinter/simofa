package dev.truewinter.simofa.routes;

import dev.truewinter.simofa.GitCredential;
import dev.truewinter.simofa.RouteLoader;
import dev.truewinter.simofa.Website;
import dev.truewinter.simofa.formvalidators.AddEditWebsiteValidator;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;

import java.sql.SQLException;
import java.util.Optional;

@SuppressWarnings("unused")
@RouteLoader.RouteClass(
        csrfErrorPage = "websites/add"
)
public class AddWebsiteRoute extends Route {
    @RouteLoader.RouteInfo(
            url = "/websites/add"
    )
    public void get(Context ctx) {
        render(ctx, "websites/add");
    }

    @RouteLoader.RouteInfo(
            url = "/websites/add",
            method = HandlerType.POST
    )
    public void post(Context ctx) {
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

        // It may be null above, but the validator ensures that it isn't
        @SuppressWarnings("ConstantConditions")
        int memory = Integer.parseInt(ctx.formParam("memory"));
        @SuppressWarnings("ConstantConditions")
        double cpu = Double.parseDouble(ctx.formParam("cpu"));

        if (error.isPresent()) {
            renderError(ctx, "websites/add", error.get());
            return;
        }

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
                0,
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

        try {
            getDatabase().getWebsiteDatabase().addWebsite(website);
            redirect(ctx,"/websites");
        } catch (SQLException e) {
            e.printStackTrace();
            renderError(ctx, "websites/add", "Failed to add website");
        }
    }
}
