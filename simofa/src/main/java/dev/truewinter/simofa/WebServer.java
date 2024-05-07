package dev.truewinter.simofa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.loader.FileLoader;
import dev.truewinter.simofa.api.SimofaPluginManager;
import dev.truewinter.simofa.api.events.WebServerStartedEvent;
import dev.truewinter.simofa.common.Util;
import dev.truewinter.simofa.config.Config;
import dev.truewinter.simofa.database.Database;
import dev.truewinter.simofa.pebble.SimofaPebbleExtension;
import dev.truewinter.simofa.routes.LoginRoute;
import dev.truewinter.simofa.routes.Route;
import dev.truewinter.simofa.routes.webhook.GithubWebhookRoute;
import io.javalin.Javalin;
import io.javalin.http.Handler;
import io.javalin.http.HandlerType;
import io.javalin.http.Header;
import io.javalin.http.staticfiles.Location;
import io.javalin.rendering.template.JavalinPebble;
import io.javalin.util.JavalinLogger;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

public class WebServer extends Thread {
    private final Config config;
    private Javalin server;

    public WebServer (Config config, Database database) {
        this.config = config;

        Route.setDatabaseInstance(database);
        Route.setConfig(config);
        loadAssetManifest();
    }

    private void loadAssetManifest() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            @SuppressWarnings("unchecked")
            Map<String, String> json = objectMapper.readValue(config.isDevMode() ?
                            new FileInputStream(new File("simofa/src/main/resources/web/assets/build/assets-manifest.json")) :
                            getClass().getClassLoader().getResourceAsStream("web/assets/build/assets-manifest.json"),
                    Map.class);
            Route.setAssetManifest(json);
        } catch (Exception e) {
            Simofa.getLogger().error("Failed to load asset manifest", e);
        }
    }

    @Override
    public void run() {
        PebbleEngine.Builder pebbleEngine = new PebbleEngine.Builder()
                .extension(new SimofaPebbleExtension());

        if (config.isDevMode()) {
            pebbleEngine.cacheActive(false)
                .templateCache(null)
                .tagCache(null)
                .loader(new FileLoader());
        }

        JavalinPebble.init(pebbleEngine.build());
        JavalinLogger.startupInfo = false;

        server = Javalin.create(c -> {
            c.showJavalinBanner = false;
            c.staticFiles.add(staticFileConfig -> {
                staticFileConfig.hostedPath = "/assets";
                staticFileConfig.directory = config.isDevMode() ?
                        "simofa/src/main/resources/web/assets" : "web/assets";
                staticFileConfig.location = config.isDevMode() ?
                        Location.EXTERNAL : Location.CLASSPATH;
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

        // A small selection of routes are registered here.
        // The rest are automatically loaded from the `routes`
        // package and registered by the RouteLoader.

        server.get("/", ctx -> {
            if (!Route.isLoggedIn(ctx)) {
                ctx.redirect("/login");
                return;
            }

            ctx.redirect("/websites");
        });

        server.get("/login", ctx -> {
           if (Route.isLoggedIn(ctx)) {
               String redirectTo = ctx.queryParam("redirectTo");
               if (Util.isBlank(redirectTo)) {
                   redirectTo = "/websites";
               }

               ctx.redirect(redirectTo);
               return;
           }

            new LoginRoute().get(ctx);
        });

        server.post("/login", ctx -> {
            if (Route.isLoggedIn(ctx)) {
                ctx.redirect("/");
                return;
            }

            new LoginRoute().post(ctx);
        });

        RouteLoader routeLoader = new RouteLoader();
        try {
            routeLoader.load(server);
        } catch (Exception e) {
            e.printStackTrace();
        }

        server.get("/builds", ctx -> {
            Route.redirect(ctx, "/builds/");
        });

        server.post("/public-api/deploy/website/{id}/github", ctx -> {
            new GithubWebhookRoute().post(ctx);
        });

        if (config.isDevMode()) {
            server.get("/_dev/reload", ctx -> {
                loadAssetManifest();
                ctx.result("OK");
            });
        }
    }

    public void registerRoute(HandlerType method, String path, Handler handler) {
        if (server == null) {
            throw new IllegalStateException("Web server not started yet");
        }

        server.addHandler(method, path, handler);
    }

    public void stopServer() {
        if (server != null) {
            server.stop();
        }
    }
}
