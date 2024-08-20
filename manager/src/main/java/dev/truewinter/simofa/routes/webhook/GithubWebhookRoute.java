package dev.truewinter.simofa.routes.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.truewinter.simofa.RouteLoader;
import dev.truewinter.simofa.SignatureVerification;
import dev.truewinter.simofa.Simofa;
import dev.truewinter.simofa.api.Website;
import dev.truewinter.simofa.common.Util;
import dev.truewinter.simofa.routes.Route;
import io.javalin.http.*;

import java.util.*;

@SuppressWarnings("unused")
@RouteLoader.RouteClass(
        verifyLogin = false
)
public class GithubWebhookRoute extends Route {
    @RouteLoader.RouteInfo(
            url = "/public-api/deploy/website/{id}/github",
            method = HandlerType.POST
    )
    public void post(Context ctx) {
        String id = ctx.pathParam("id");

        try {
            Optional<Website> website = getDatabase().getWebsiteDatabase().getWebsiteById(id);
            if (website.isEmpty()) return;

            String signature = ctx.header("X-Hub-Signature-256");
            String event = ctx.header("X-GitHub-Event");

            if (Util.isBlank(signature)) {
                throw new ForbiddenResponse("Signature missing");
            }

            String json = ctx.body();
            if (!SignatureVerification.verifyHmacSha256(json, signature, website.get().getDeployToken())) {
                throw new ForbiddenResponse("Invalid signature");
            }

            switch (Objects.requireNonNull(event)) {
                case "push":
                    if (!isCorrectBranch(website.get(), json)) return;

                    GithubAppWebhookRoute.handlePush(json, w ->
                            w.getId().equals(id) && w.getBuildOn().equals(Website.BUILD_ON.COMMIT)
                    );
                    break;
                case "create":
                    GithubAppWebhookRoute.handleTag(json, w ->
                            w.getId().equals(id) && w.getBuildOn().equals(Website.BUILD_ON.TAG));
                    break;
                case "release":
                    GithubAppWebhookRoute.handleRelease(json, w ->
                            w.getId().equals(id) && w.getBuildOn().equals(Website.BUILD_ON.RELEASE));
                    break;
            }
        } catch (Exception e) {
            Simofa.getLogger().error("An error occurred while processing GitHub webhook", e);
            throw new InternalServerErrorResponse("An error occurred");
        }
    }

    private boolean isCorrectBranch(Website website, String json) throws JsonProcessingException {
        HashMap<String, Object> data = GithubAppWebhookRoute.getData(json);

        String ref = (String) data.get("ref");
        if (Util.isBlank(ref) || !ref.startsWith("refs/heads/")) return false;

        String branch = ref.replace("refs/heads/", "");
        return branch.equals(website.getGitBranch());
    }
}
