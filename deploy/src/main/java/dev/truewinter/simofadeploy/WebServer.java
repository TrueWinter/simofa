package dev.truewinter.simofadeploy;

import dev.truewinter.simofa.common.Util;
import io.javalin.Javalin;
import io.javalin.http.Header;
import io.javalin.http.UploadedFile;
import io.javalin.jetty.JavalinJettyServlet;
import io.javalin.jetty.JettyUtil;
import io.javalin.util.JavalinLogger;
import org.apache.commons.codec.cli.Digest;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.Base64;
import java.util.Objects;

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
            String buildUrl = ctx.formParam("build_url");
            String buildId = ctx.formParam("build_id");
            String deployCmd = ctx.formParam("deployment_command");
            String deployFailedCmd = ctx.formParam("deployment_failed_command");
            String siteHash = ctx.formParam("site_hash");
            UploadedFile site = ctx.uploadedFile("site");

            if (Util.isBlank(key) || Util.isBlank(buildUrl) || Util.isBlank(buildId) ||
                    Util.isBlank(deployCmd) || Util.isBlank(deployFailedCmd) || Util.isBlank(siteHash)) {
                ctx.status(403).result("Required fields missing from request");
                return;
            }

            if (!Util.secureCompare(key, config.getKey())) {
                ctx.status(403).result("Invalid key");
                return;
            }

            if (!Util.isValidUrl(buildUrl)) {
                ctx.status(400).result("build_url is not a valid URL");
                return;
            }

            if (site == null || site.size() == 0) {
                ctx.status(400).result("Site is required");
                return;
            }

            if (!site.extension().equals(".zip")) {
                ctx.status(400).result("Simofa Deploy only supports .zip files");
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

            File tmpDir = null;
            try {
                tmpDir = Util.createTempDir(buildId);
                File tmpInDir = new File(tmpDir, "in");
                File tmpScriptsDir = new File(tmpDir, "scripts");

                File siteFile = new File(tmpInDir, WebsiteDeployment.SITE_ZIP_NAME);
                try (OutputStream outputStream = new FileOutputStream(siteFile)) {
                    IOUtils.copy(site.content(), outputStream);
                }

                try (InputStream siteFileReadBack = new FileInputStream(siteFile)) {
                    if (!siteHash.equals(DigestUtils.md5Hex(siteFileReadBack))) {
                        throw new Exception("Hashes do not match");
                    }
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
