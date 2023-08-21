package dev.truewinter.simofa.routes.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.truewinter.simofa.SignatureVerification;
import dev.truewinter.simofa.Simofa;
import dev.truewinter.simofa.Website;
import dev.truewinter.simofa.common.Util;
import dev.truewinter.simofa.routes.Route;
import io.javalin.http.Context;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Optional;

public class GithubWebhookRoute extends Route {
    @Override
    public void post(Context ctx) {
        int websiteId = Integer.parseInt(ctx.pathParam("id"));
        HashMap<String, Object> resp = new HashMap<>();
        resp.put("success", true);

        try {
            Optional<Website> website = getDatabase().getWebsiteDatabase().getWebsiteById(websiteId);
            String signature = ctx.header("X-Hub-Signature-256");

            if (Util.isBlank(signature)) {
                resp.put("success", false);
                resp.put("error", "Signature missing");
                ctx.status(403).json(resp);
                return;
            }

            if (website.isPresent()) {
                String json = ctx.body();
                ObjectMapper objectMapper = new ObjectMapper();

                @SuppressWarnings("unchecked")
                HashMap<String, Object> data = objectMapper.readValue(json, HashMap.class);
                String ref = (String) data.get("ref");
                if (Util.isBlank(ref) || !ref.startsWith("refs/heads/")) {
                    resp.put("success", false);
                    resp.put("error", "Not a branch");
                    ctx.status(400).json(resp);
                    return;
                }

                String branch = ref.replace("refs/heads/", "");
                if (!website.get().getGitBranch().equals(branch)) {
                    resp.put("success", false);
                    resp.put("error", "Website is not configured for this branch");
                    ctx.status(400).json(resp);
                    return;
                }

                if (!SignatureVerification.verifyHmacSha256(json, signature, website.get().getDeployToken())) {
                    resp.put("success", false);
                    resp.put("error", "Invalid signature");
                    ctx.status(403).json(resp);
                    return;
                }

                String commit = "<unknown>";
                @SuppressWarnings("unchecked")
                ArrayList<LinkedHashMap<String, Object>> commits = (ArrayList<LinkedHashMap<String, Object>>) data.get("commits");
                if (commits.size() > 0) {
                    commit = (String) commits.get(0).get("message");
                }

                Simofa.getBuildQueueManager().getBuildQueue().queue(website.get(), commit);
                ctx.json(resp);
            } else {
                resp.put("success", false);
                resp.put("error", "Website not found");
                ctx.status(404).json(resp);
            }
        } catch (Exception e) {
            Simofa.getLogger().error("An error occurred while processing GitHub webhook", e);
            resp.put("success", false);
            resp.put("error", "An error occurred");
            ctx.status(500).json(resp);
        }
    }
}
