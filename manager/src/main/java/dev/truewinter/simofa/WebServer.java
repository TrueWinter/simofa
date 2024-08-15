package dev.truewinter.simofa;

import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.loader.ClasspathLoader;
import com.mitchellbosecke.pebble.loader.Loader;
import dev.truewinter.simofa.api.SimofaPluginManager;
import dev.truewinter.simofa.api.events.WebServerStartedEvent;
import dev.truewinter.simofa.api.internal.WsRegistry;
import dev.truewinter.simofa.common.Util;
import dev.truewinter.simofa.config.Config;
import dev.truewinter.simofa.database.Database;
import dev.truewinter.simofa.pebble.SimofaPebbleExtension;
import dev.truewinter.simofa.routes.Route;
import dev.truewinter.simofa.routes.BuildLogsWsRoute;
import dev.truewinter.simofa.routes.QueueWsRoute;
import dev.truewinter.simofa.routes.ingest.DeployServerIngestWsRoute;
import dev.truewinter.simofa.routes.webhook.GithubWebhookRoute;
import io.javalin.Javalin;
import io.javalin.http.Handler;
import io.javalin.http.HandlerType;
import io.javalin.http.Header;
import io.javalin.rendering.template.JavalinPebble;
import io.javalin.util.JavalinLogger;

public class WebServer extends Thread {
    private final Config config;
    private Javalin server;

    public WebServer (Config config, Database database) {
        this.config = config;

        Route.setDatabaseInstance(database);
    }

    @Override
    public void run() {
        PebbleEngine.Builder pebbleEngine = new PebbleEngine.Builder()
                .extension(new SimofaPebbleExtension());
        Loader<String> loader = new ClasspathLoader();
        loader.setPrefix("web/html");
        loader.setSuffix(".peb");
        pebbleEngine.loader(loader);
        JavalinLogger.startupInfo = false;

        server = Javalin.create(c -> {
            c.showJavalinBanner = false;
            // In development, the dist folder is empty which prevents it from being included as a resource
            if (getClass().getResource("/web/dist") != null) {
                c.staticFiles.add(s -> {
                    s.hostedPath = "/assets";
                    s.directory = "/web/dist";
                });
            }
            c.fileRenderer(new JavalinPebble(pebbleEngine.build()));
            c.spaRoot.addHandler("/", ctx -> {
                ctx.render("index");
            });
        }).start(config.getPort());

        SimofaPluginManager.getInstance().getPluginManager().fireEvent(new WebServerStartedEvent());

        server.before(ctx -> {
            ctx.header("X-Robots-Tag", "noindex");
            ctx.header(Header.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
            ctx.header(Header.EXPIRES, "0");

            try {
                ctx.header("X-Powered-By", "Simofa/" + Util.getVersion());
            } catch (Exception e) {
                ctx.header("X-Powered-By", "Simofa");
            }
        });

        /*
            A small selection of routes are registered here.
            The rest are automatically loaded from the `routes`
            package and registered by the RouteLoader.
         */

        RouteLoader routeLoader = new RouteLoader();
        try {
            routeLoader.load(server);
        } catch (Exception e) {
            Simofa.getLogger().error("Failed to register routes", e);
        }

        server.post("/public-api/deploy/website/{id}/github", ctx -> {
            new GithubWebhookRoute().post(ctx);
        });

        server.ws("/api/ws/websites/{websiteId}/builds/{buildId}/logs", ws -> {
            WsRegistry.registerWsConsumer(WsRegistry.Instances.WEBSITE_LOGS, BuildLogsWsRoute::sendNewLog);
            WsManager.init(ws, BuildLogsWsRoute.class);
        });

        server.ws("/api/ws/queue", ws -> {
            WsRegistry.registerWsConsumer(WsRegistry.Instances.BUILD_QUEUE, QueueWsRoute::sendUpdate);
            WsManager.init(ws, QueueWsRoute.class);
        });

        server.ws("/api/ws/ingest/deploy", ws -> WsManager.init(ws, DeployServerIngestWsRoute.class));
    }

    public void registerRoute(HandlerType method, String path, Handler handler) {
        if (server == null) {
            throw new IllegalStateException("Web server not started yet");
        }

        server.addHandler(method, path, handler);
        Simofa.getLogger().info("Registered plugin route " + path);
    }

    public void stopServer() {
        if (server != null) {
            server.stop();
        }
    }
}
