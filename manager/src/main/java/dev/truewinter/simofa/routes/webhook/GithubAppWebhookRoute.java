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

import java.sql.SQLException;
import java.util.*;
import java.util.function.Predicate;

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
            String event = ctx.header("X-GitHub-Event");

            if (Util.isBlank(signature)) {
                throw new ForbiddenResponse("Signature missing");
            }

            String json = ctx.body();
            if (!SignatureVerification.verifyHmacSha256(json, signature, getConfig().getGithubAppSecret())) {
                throw new ForbiddenResponse("Invalid signature");
            }

            HashMap<String, Object> data = getData(json);
            @SuppressWarnings("unchecked")
            String repository = (String) ((HashMap<String, Object>) data.get("repository")).get("full_name");

            switch (Objects.requireNonNull(event)) {
                case "push":
                    String ref = (String) data.get("ref");
                    if (Util.isBlank(ref) || !ref.startsWith("refs/heads/")) return;
                    String branch = ref.replace("refs/heads/", "");

                    handlePush(json, w ->
                            w.getGitUrl().toLowerCase().contains(repository.toLowerCase()) &&
                                    w.getGitBranch().equals(branch) &&
                                    w.getBuildOn().equals(Website.BUILD_ON.COMMIT)
                    );
                    break;
                case "create":
                    handleTag(json, w ->
                            w.getGitUrl().toLowerCase().contains(repository.toLowerCase()) &&
                                    w.getBuildOn().equals(Website.BUILD_ON.TAG));
                    break;
                case "release":
                    handleRelease(json, w ->
                            w.getGitUrl().toLowerCase().contains(repository.toLowerCase()) &&
                                    w.getBuildOn().equals(Website.BUILD_ON.RELEASE));
                    break;
            }
        } catch (Exception e) {
            Simofa.getLogger().error("An error occurred while processing GitHub webhook", e);
            throw new InternalServerErrorResponse("An error occurred");
        }
    }

    public static void handlePush(String json, Predicate<Website> predicate) throws JsonProcessingException, SQLException {
        HashMap<String, Object> data = getData(json);

        Optional<Website> website = getDatabase().getWebsiteDatabase().getWebsites().stream()
                .filter(predicate).findFirst();

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
    }

    public static void handleTag(String json, Predicate<Website> predicate) throws JsonProcessingException, SQLException {
        HashMap<String, Object> data = getData(json);
        String ref = (String) data.get("ref");
        String refType = (String) data.get("ref_type");

        if (!refType.equals("tag")) return;

        Optional<Website> website = getDatabase().getWebsiteDatabase().getWebsites().stream()
                .filter(predicate).findFirst();

        if (website.isEmpty()) return;

        website.ifPresent(w -> {
            String tag = ref.replace("refs/heads/", "");
            Simofa.getBuildQueueManager().getBuildQueue().queue(w, tag);
        });
    }

    public static void handleRelease(String json, Predicate<Website> predicate) throws JsonProcessingException, SQLException {
        HashMap<String, Object> data = getData(json);
        String action = (String) data.get("action");

        if (!action.equals("released")) return;

        @SuppressWarnings("unchecked")
        String repository = (String) ((HashMap<String, Object>) data.get("repository")).get("full_name");

        Optional<Website> website = getDatabase().getWebsiteDatabase().getWebsites().stream()
                .filter(predicate).findFirst();

        if (website.isEmpty()) return;

        website.ifPresent(w -> {
            @SuppressWarnings("unchecked")
            String title = (String) ((HashMap<String, Object>) data.get("release")).get("name");
            Simofa.getBuildQueueManager().getBuildQueue().queue(w, title);
        });
    }

    public static HashMap<String, Object> getData(String json) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        HashMap<String, Object> data = objectMapper.readValue(json, HashMap.class);
        return data;
    }
}
