package dev.truewinter.simofa.formvalidators;

import dev.truewinter.simofa.common.Util;
import io.javalin.http.Context;

import java.util.Optional;

public class AddEditWebsiteValidator implements FormValidator {
    @Override
    public Optional<String> hasError(Context ctx) {
        String name = ctx.formParam("name");
        String dockerImage = ctx.formParam("docker_image");
        String memory = ctx.formParam("memory");
        String cpu = ctx.formParam("cpu");
        String gitUrl = ctx.formParam("git_url");
        String gitBranch = ctx.formParam("git_branch");
        String gitCredential = ctx.formParam("git_credential");
        String buildCommand = ctx.formParam("build_command");
        String deploymentCommand = ctx.formParam("deployment_command");
        String deploymentFailedCommand = ctx.formParam("deployment_failed_command");
        String deploymentServer = ctx.formParam("deployment_server");
        String deployToken = ctx.formParam("deploy_token");

        if (Util.isBlank(name) || Util.isBlank(dockerImage) || Util.isBlank(memory) ||
                Util.isBlank(cpu) || Util.isBlank(gitUrl) || Util.isBlank(gitBranch) ||
                Util.isBlank(gitCredential) || Util.isBlank(buildCommand) ||
                Util.isBlank(deploymentCommand) || Util.isBlank(deploymentFailedCommand) ||
                Util.isBlank(deploymentServer) || Util.isBlank(deployToken)) {
            return Optional.of("Required fields must be filled in");
        }

        if (!memory.matches("^[0-9]+$")) {
            return Optional.of("Invalid memory value");
        }

        if (!cpu.matches("^[0-9]{0,2}(?:\\.[0-9]{0,2})?$")) {
            return Optional.of("Invalid CPU value");
        }

        return Optional.empty();
    }
}
