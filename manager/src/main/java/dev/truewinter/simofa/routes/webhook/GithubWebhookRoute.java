package dev.truewinter.simofa.routes.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
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

public class GithubWebhookRoute extends Route {
    public void post(Context ctx) {
        String websiteId = ctx.pathParam("id");
        HashMap<String, Object> resp = new HashMap<>();
        resp.put("success", true);

        try {
            Optional<Website> website = getDatabase().getWebsiteDatabase().getWebsiteById(websiteId);
            String signature = ctx.header("X-Hub-Signature-256");

            if (Util.isBlank(signature)) {
                throw new ForbiddenResponse("Signature missing");
            }

            if (website.isPresent()) {
                String json = ctx.body();
                ObjectMapper objectMapper = new ObjectMapper();

                @SuppressWarnings("unchecked")
                HashMap<String, Object> data = objectMapper.readValue(json, HashMap.class);
                String ref = (String) data.get("ref");
                if (Util.isBlank(ref) || !ref.startsWith("refs/heads/")) {
                    throw new BadRequestResponse("Not a branch");
                }

                String branch = ref.replace("refs/heads/", "");
                if (!website.get().getGitBranch().equals(branch)) {
                    throw new BadRequestResponse("Website is not configured for this branch");
                }

                if (!SignatureVerification.verifyHmacSha256(json, signature, website.get().getDeployToken())) {
                    throw new ForbiddenResponse("Invalid signature");
                }

                String commit = "<unknown>";
                @SuppressWarnings("unchecked")
                ArrayList<LinkedHashMap<String, Object>> commits = (ArrayList<LinkedHashMap<String, Object>>) data.get("commits");
                if (!commits.isEmpty()) {
                    commit = (String) commits.get(0).get("message");
                }

                Simofa.getBuildQueueManager().getBuildQueue().queue(website.get(), commit);
                ctx.json(resp);
            } else {
                throw new NotFoundResponse("Website not found");
            }
        } catch (Exception e) {
            Simofa.getLogger().error("An error occurred while processing GitHub webhook", e);
            throw new InternalServerErrorResponse("An error occurred");
        }
    }
}
