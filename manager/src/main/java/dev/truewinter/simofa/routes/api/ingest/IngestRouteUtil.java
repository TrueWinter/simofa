package dev.truewinter.simofa.routes.api.ingest;

import dev.truewinter.simofa.WsToken;
import dev.truewinter.simofa.api.internal.WsRegistry;
import org.apache.hc.core5.net.URIBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.function.BiFunction;

public class IngestRouteUtil {
    private static final HashMap<WsRegistry.IngestInstances, RouteConfig> routes = new HashMap<>(){{
        put(WsRegistry.IngestInstances.DEPLOY_INGEST, new RouteConfig("deploy", DeployServerIngestWsRoute::getRoom));
    }};

    public static String createWsUrl(boolean secure, String domain, WsRegistry.IngestInstances ingestFrom,
                                     String websiteId, String buildId) throws URISyntaxException {
        RouteConfig route = routes.get(ingestFrom);
        if (route == null) {
            throw new IllegalArgumentException("Ingest route not found");
        }

        WsToken token = new WsToken(route.getRoomMethod().apply(websiteId, buildId), 60 * 60);

        URI uri = new URIBuilder()
                .setScheme(secure ? "wss" : "ws")
                .setHost(domain)
                .setPath("/api/ws/ingest/" + route.route())
                .addParameter("website", websiteId)
                .addParameter("build", buildId)
                .addParameter("token", token.getJWT())
                .build();

        return uri.toString();
    }

    private record RouteConfig(String route, BiFunction<String, String, String> getRoomMethod) {}
}
