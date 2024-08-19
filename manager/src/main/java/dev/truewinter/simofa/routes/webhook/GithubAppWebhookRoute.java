package dev.truewinter.simofa.routes.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.truewinter.simofa.RouteLoader;
import dev.truewinter.simofa.SignatureVerification;
import dev.truewinter.simofa.Simofa;
import dev.truewinter.simofa.api.Website;
import dev.truewinter.simofa.common.Util;
import dev.truewinter.simofa.routes.Route;
import io.javalin.http.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Optional;

@SuppressWarnings("unused")
@RouteLoader.RouteClass(
        verifyLogin = false
)
public class GithubAppWebhookRoute extends Route {
    @RouteLoader.RouteInfo(
            url = "/public-api/deploy/github",
            method = HandlerType.POST
    )
    public void post(Context ctx) {
        try {
            String signature = ctx.header("X-Hub-Signature-256");

            if (Util.isBlank(signature)) {
                throw new ForbiddenResponse("Signature missing");
            }

            String json = ctx.body();
            if (!SignatureVerification.verifyHmacSha256(json, signature, getConfig().getGithubAppSecret())) {
                throw new ForbiddenResponse("Invalid signature");
            }

            ObjectMapper objectMapper = new ObjectMapper();

            @SuppressWarnings("unchecked")
            HashMap<String, Object> data = objectMapper.readValue(json, HashMap.class);
            String ref = (String) data.get("ref");
            if (Util.isBlank(ref) || !ref.startsWith("refs/heads/")) return;

            @SuppressWarnings("unchecked")
            String repository = (String) ((HashMap<String, Object>) data.get("repository")).get("full_name");
            String branch = ref.replace("refs/heads/", "");

            Optional<Website> website = getDatabase().getWebsiteDatabase().getWebsites().stream().filter(w ->
                    w.getGitUrl().toLowerCase().contains(repository.toLowerCase()) && w.getGitBranch().equals(branch)
            ).findFirst();

            if (website.isEmpty()) return;

            website.ifPresent(w -> {
                String commit = "<unknown>";
                @SuppressWarnings("unchecked")
                ArrayList<LinkedHashMap<String, Object>> commits = (ArrayList<LinkedHashMap<String, Object>>) data.get("commits");
                if (!commits.isEmpty()) {
                    commit = (String) commits.get(0).get("message");
                }

                Simofa.getBuildQueueManager().getBuildQueue().queue(w, commit);
            });
        } catch (Exception e) {
            Simofa.getLogger().error("An error occurred while processing GitHub webhook", e);
            throw new InternalServerErrorResponse("An error occurred");
        }
    }
}
