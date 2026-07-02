package dev.truewinter.simofa.deploy;

import dev.truewinter.simofa.common.Util;
import io.javalin.Javalin;
import io.javalin.http.Header;
import io.javalin.util.JavalinLogger;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class WebServer extends Thread {
    private final Config config;
    private Javalin server;

    public WebServer(Config config) {
        this.config = config;
    }

    @Override
    public void run() {
        JavalinLogger.startupInfo = false;
        server = Javalin.create(c -> {
            c.showJavalinBanner = false;
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

        server.post("/deploy", ctx -> {
            String key = ctx.formParam("key");
            String buildUrl = ctx.formParam("ws_deploy_url");
            String buildId = ctx.formParam("build_id");
            String deployCmd = ctx.formParam("deployment_command");
            String deployFailedCmd = ctx.formParam("deployment_failed_command");
            String siteHash = ctx.formParam("site_hash");
            String s3PresignedUrl = ctx.formParam("s3_presigned_url");

            if (Util.isBlank(key) || Util.isBlank(buildUrl) || Util.isBlank(buildId) ||
                    Util.isBlank(deployCmd) || Util.isBlank(deployFailedCmd) || Util.isBlank(siteHash) || Util.isBlank(s3PresignedUrl)) {
                ctx.status(403).result("Required fields missing from request");
                return;
            }

            if (!Util.secureCompare(key, config.getKey())) {
                ctx.status(403).result("Invalid key");
                return;
            }

            if (!Util.isValidUri(buildUrl)) {
                ctx.status(400).result("ws_deploy_url is not a valid URL");
                return;
            }

            String deployCmd2 = Util.base64Decode(deployCmd);
            String deployFailedCmd2 = Util.base64Decode(deployFailedCmd);

            if (deployCmd2 == null || deployFailedCmd2 == null) {
                ctx.status(400).result("Commands must be Base64 encoded");
                return;
            }

            if (!deployCmd2.startsWith("#!") || !deployFailedCmd2.startsWith("#!")) {
                ctx.status(400).result("Commands must be valid bash scripts, starting with a shebang");
                return;
            }

            if (siteHash == null) {
                ctx.status(400).result("site_hash is null");
                return;
            }

            if (s3PresignedUrl == null) {
                ctx.status(400).result("s3_presigned_url is null");
                return;
            }

            File tmpDir = null;
            try {
                tmpDir = Util.createTempDir(buildId);
                File tmpInDir = new File(tmpDir, "in");
                File tmpScriptsDir = new File(tmpDir, "scripts");

                File siteFile = new File(tmpInDir, WebsiteDeployment.SITE_ZIP_NAME);
                
                // Download from presigned URL using apache HttpClient
                HttpClient client =  HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(s3PresignedUrl)).build();
                client.send(request, HttpResponse.BodyHandlers.ofFile(siteFile.toPath()));

                // Validate hash
                if (!siteHash.equals(DigestUtils.md5Hex(new FileInputStream(siteFile)))) {
                    throw new Exception("Hashes do not match");
                }

                File deployCmdFile = new File(tmpScriptsDir, WebsiteDeployment.DEPLOY_CMD_NAME);
                try (FileWriter deployCmdWriter = new FileWriter(deployCmdFile)) {
                    deployCmdWriter.write(deployCmd2);
                    if (!deployCmdFile.setExecutable(true)) {
                        throw new Exception("Failed to set deploy script executable bit");
                    }
                }

                File deployFailedCmdFile = new File(tmpScriptsDir, WebsiteDeployment.DEPLOY_FAILED_CMD_NAME);
                try (FileWriter deployFailedCmdWriter = new FileWriter(deployFailedCmdFile)) {
                    deployFailedCmdWriter.write(deployFailedCmd2);
                    if (!deployFailedCmdFile.setExecutable(true)) {
                        throw new Exception("Failed to set deployment failed script executable bit");
                    }
                }

                SimofaDeploy.getDeployQueueManager().getDeployQueue().queue(
                        new WebsiteDeployment(buildId, buildUrl, key, tmpDir)
                );

                ctx.result("Deployment queued");
            } catch (Exception e) {
                ctx.status(500).result("Failed to deploy");
                SimofaDeploy.getLogger().error(String.format("Failed to deploy build %s", buildId), e);
                if (tmpDir != null) {
                    try {
                        FileUtils.deleteDirectory(tmpDir);
                    } catch (Exception ex) {
                        SimofaDeploy.getLogger().warn("Failed to delete temporary directory", ex);
                    }
                }
            }
        });

        server.get("/status", ctx -> {
            ctx.status(200).result("OK");
        });
    }

    public void stopServer() {
        if (server != null) {
            server.stop();
        }
    }
}
