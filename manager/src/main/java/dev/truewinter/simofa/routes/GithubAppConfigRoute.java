package dev.truewinter.simofa.routes;

import dev.truewinter.simofa.RouteLoader;
import dev.truewinter.simofa.Simofa;
import dev.truewinter.simofa.common.Util;
import dev.truewinter.simofa.config.Config;
import io.javalin.http.Context;
import org.apache.hc.core5.net.URIBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

@SuppressWarnings("unused")
@RouteLoader.RouteClass
public class GithubAppConfigRoute extends Route {
    @RouteLoader.RouteInfo(
            url = "/api/config/github-app"
    )
    public void get(Context ctx) throws URISyntaxException {
        Config config = getConfig();
        URI uri = new URIBuilder()
                .setScheme(config.isRemoteUrlSecure() ? "https" : "http")
                .setHost(config.getRemoteUrlDomain())
                .setPath("/public-api/deploy/github")
                .build();

        HashMap<String, String> resp = new HashMap<>();
        resp.put("url", uri.toString());
        resp.put("secret", config.getGithubAppSecret());
        resp.put("random", Util.generateRandomString(10));

        ctx.json(resp);
    }
}
