package dev.truewinter.simofa.routes.api.ingest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.truewinter.simofa.Simofa;
import dev.truewinter.simofa.api.WebsiteBuild;
import dev.truewinter.simofa.api.internal.WsRegistry;
import dev.truewinter.simofa.common.BuildStatus;
import dev.truewinter.simofa.common.SimofaLog;
import dev.truewinter.simofa.common.Util;
import dev.truewinter.simofa.routes.WsRoute;
import io.javalin.websocket.WsContext;
import io.javalin.websocket.WsMessageContext;

import java.util.List;
import java.util.Optional;

public class DeployServerIngestWsRoute extends WsRoute {
    private WebsiteBuild build;

    public DeployServerIngestWsRoute(WsContext ctx) {
        super(ctx);

        String websiteId = ctx.queryParam("website");
        String buildId = ctx.queryParam("build");

        if (!Util.isBlank(websiteId, buildId)) {
            Optional<WebsiteBuild> build = getBuild(websiteId, buildId);
            build.ifPresent(b -> this.build = b);
        }
    }

    @Override
    public void handle() {
        if (build == null) {
            sendEvent("error", "Build not found");
            ctx.closeSession();
        }
    }

    @Override
    public void handleMessage(WsMessageContext ctx) {
        try {
            JsonNode json = new ObjectMapper().readTree(ctx.message());
            String type = json.get("type").asText();
            JsonNode data = json.get("data");

            switch (type) {
                case "log":
                    SimofaLog log = new ObjectMapper().readValue(data.toString(), SimofaLog.class);
                    build.addLog(log);
                    break;
                case "status":
                    build.setStatus(BuildStatus.valueOf(data.asText()));
                    break;
            }
        } catch (JsonProcessingException e) {
            Simofa.getLogger().error("Failed to parse message from deploy server", e);
        }
    }

    @Override
    public String getRoom() {
        String websiteId = ctx.queryParam("website");
        String buildId = ctx.queryParam("build");
        return getRoom(websiteId, buildId);
    }

    public static String getRoom(String websiteId, String buildId) {
        return String.format("%s-%s-%s", WsRegistry.IngestInstances.DEPLOY_INGEST, websiteId, buildId);
    }

    private Optional<WebsiteBuild> getBuild(String websiteId, String buildId) {
        List<WebsiteBuild> builds = Simofa.getBuildQueueManager().getBuildQueue().getWebsiteBuildList().get(websiteId);

        if (builds == null) {
            return Optional.empty();
        }

        return builds.stream().filter(b ->
                b.getWebsite().getId().equals(websiteId) && b.getId().equals(buildId)).findFirst();
    }
}
