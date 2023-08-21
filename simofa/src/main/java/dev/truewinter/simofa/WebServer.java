package dev.truewinter.simofa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.loader.FileLoader;
import dev.truewinter.simofa.common.Util;
import dev.truewinter.simofa.config.Config;
import dev.truewinter.simofa.database.Database;
import dev.truewinter.simofa.pebble.SimofaPebbleExtension;
import dev.truewinter.simofa.routes.*;
import dev.truewinter.simofa.routes.api.*;
import dev.truewinter.simofa.routes.api.deploy.LogReceiverAPIRoute;
import dev.truewinter.simofa.routes.api.deploy.StatusReceiverAPIRoute;
import dev.truewinter.simofa.routes.webhook.GithubWebhookRoute;
import io.javalin.Javalin;
import io.javalin.http.Header;
import io.javalin.http.staticfiles.Location;
import io.javalin.rendering.template.JavalinPebble;
import io.javalin.util.JavalinLogger;
import io.javalin.websocket.WsConnectContext;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

        server.get("/logout", ctx -> {
            new LogoutRoute().verifyLogin(ctx).get(ctx);
        });

        server.get("/websites", ctx -> {
            new WebsitesRoute().verifyLogin(ctx).get(ctx);
        });

        server.get("/websites/add", ctx -> {
            new AddWebsiteRoute().verifyLogin(ctx).get(ctx);
        });

        server.post("/websites/add", ctx -> {
            new AddWebsiteRoute().verifyLogin(ctx).verifyCSRF(ctx, "websites/add").post(ctx);
        });

        server.get("/websites/{id}/edit", ctx -> {
            new EditWebsiteRoute().verifyLogin(ctx).get(ctx);
        });

        server.post("/websites/{id}/edit", ctx -> {
            new EditWebsiteRoute().verifyLogin(ctx).verifyCSRF(ctx, "error").post(ctx);
        });

        server.post("/websites/{id}/delete", ctx -> {
            new DeleteWebsiteRoute().verifyLogin(ctx).verifyCSRF(ctx, "error").post(ctx);
        });

        server.post("/websites/{id}/pull", ctx -> {
            new PullWebsiteRoute().verifyLogin(ctx).verifyCSRF(ctx, "error").post(ctx);
        });

        server.get("/websites/{id}/logs", ctx -> {
            new LogsRoute().verifyLogin(ctx).get(ctx);
        });

        server.get("/websites/{wid}/build/{bid}/logs", ctx -> {
            new BuildLogsRoute().verifyLogin(ctx).get(ctx);
        });

        server.post("/websites/{wid}/build/{bid}/stop", ctx -> {
            new StopBuildRoute().verifyLogin(ctx).verifyCSRF(ctx, "error").post(ctx);
        });

        server.get("/deployment-servers", ctx -> {
            new DeploymentServersRoute().verifyLogin(ctx).get(ctx);
        });

        server.get("/deployment-servers/add", ctx -> {
            new AddDeploymentServerRoute().verifyLogin(ctx).get(ctx);
        });

        server.post("/deployment-servers/add", ctx -> {
            new AddDeploymentServerRoute().verifyLogin(ctx).verifyCSRF(ctx, "deployment-servers/add").post(ctx);
        });

        server.get("/deployment-servers/{id}/edit", ctx -> {
            new EditDeploymentServerRoute().verifyLogin(ctx).get(ctx);
        });

        server.post("/deployment-servers/{id}/edit", ctx -> {
            new EditDeploymentServerRoute().verifyLogin(ctx).verifyCSRF(ctx, "error").post(ctx);
        });

        server.post("/deployment-servers/{id}/delete", ctx -> {
            new DeleteDeploymentServerRoute().verifyLogin(ctx).verifyCSRF(ctx, "error").post(ctx);
        });

        server.get("/accounts", ctx -> {
            new AccountsRoute().verifyLogin(ctx).get(ctx);
        });

        server.get("/accounts/add", ctx -> {
            new AddAccountRoute().verifyLogin(ctx).get(ctx);
        });

        server.post("/accounts/add", ctx -> {
            new AddAccountRoute().verifyLogin(ctx).verifyCSRF(ctx, "accounts/add").post(ctx);
        });

        server.get("/accounts/{id}/edit", ctx -> {
            new EditAccountRoute().verifyLogin(ctx).get(ctx);
        });

        server.post("/accounts/{id}/edit", ctx -> {
            new EditAccountRoute().verifyLogin(ctx).verifyCSRF(ctx, "error").post(ctx);
        });

        server.post("/accounts/{id}/delete", ctx -> {
           new DeleteAccountRoute().verifyLogin(ctx).verifyCSRF(ctx, "error").post(ctx);
        });

        server.post("/docker/containers/{id}/delete", ctx -> {
            new DeleteDockerContainerRoute().verifyLogin(ctx).verifyCSRF(ctx, "error").post(ctx);
        });

        server.get("/docker/*", ctx -> {
            new DockerRoute().verifyLogin(ctx).get(ctx);
        });

        server.get("/docker", ctx -> {
            Route.redirect(ctx, "/docker/");
        });

        server.get("/git", ctx -> {
            new GitRoute().verifyLogin(ctx).get(ctx);
        });

        server.get("/git/add", ctx -> {
            new AddGitRoute().verifyLogin(ctx).get(ctx);
        });

        server.post("/git/add", ctx -> {
            new AddGitRoute().verifyLogin(ctx).verifyCSRF(ctx, "error").post(ctx);
        });

        server.get("/git/{id}/edit", ctx -> {
            new EditGitRoute().verifyLogin(ctx).get(ctx);
        });

        server.post("/git/{id}/edit", ctx -> {
            new EditGitRoute().verifyLogin(ctx).verifyCSRF(ctx, "error").post(ctx);
        });

        server.post("/git/{id}/delete", ctx -> {
            new DeleteGitRoute().verifyLogin(ctx).verifyCSRF(ctx, "error").post(ctx);
        });

        server.get("/api/deployment-servers", ctx -> {
            new DeploymentServersAPIRoute().verifyLogin(ctx).get(ctx);
        });

        server.get("/api/websites", ctx -> {
            new WebsitesAPIRoute().verifyLogin(ctx).get(ctx);
        });

        server.get("/api/websites/{wid}/build/{bid}/logs", ctx -> {
            new BuildLogsAPIRoute().verifyLogin(ctx).get(ctx);
        });

        server.post("/api/websites/{wid}/build/{bid}/logs/add", ctx -> {
            new LogReceiverAPIRoute().post(ctx);
        });

        server.post("/api/websites/{wid}/build/{bid}/status/set", ctx -> {
            new StatusReceiverAPIRoute().post(ctx);
        });

        server.get("/api/queue", ctx -> {
            new QueueAPI().verifyLogin(ctx).get(ctx);
        });

        server.get("/api/accounts", ctx -> {
            new AccountsAPIRoute().verifyLogin(ctx).get(ctx);
        });

        server.get("/api/docker/images", ctx -> {
            new DockerImagesAPIRoute().verifyLogin(ctx).get(ctx);
        });

        server.get("/api/docker/containers", ctx -> {
            new DockerContainersAPIRoute().verifyLogin(ctx).get(ctx);
        });

        server.get("/api/templates", ctx -> {
            new TemplatesAPIRoute().verifyLogin(ctx).get(ctx);
        });

        server.post("/api/templates/add", ctx -> {
            new AddTemplateAPIRoute().verifyLogin(ctx).verifyCSRF(ctx, "error").post(ctx);
        });

        server.post("/api/templates/{id}/delete", ctx -> {
            new DeleteTemplateAPIRoute().verifyLogin(ctx).verifyCSRF(ctx, "error").post(ctx);
        });

        server.get("/api/git", ctx -> {
            new GitAPIRoute().verifyLogin(ctx).get(ctx);
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

    public void stopServer() {
        if (server != null) {
            server.stop();
        }
    }
}
