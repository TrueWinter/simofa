package dev.truewinter.simofa.docker;

import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;
import com.github.dockerjava.api.model.WaitResponse;
import dev.truewinter.simofa.api.DeploymentServer;
import dev.truewinter.simofa.GitFetcher;
import dev.truewinter.simofa.Simofa;
import dev.truewinter.simofa.api.WebsiteBuild;
import dev.truewinter.simofa.common.BuildStatus;
import dev.truewinter.simofa.common.LogType;
import dev.truewinter.simofa.common.SimofaLog;
import dev.truewinter.simofa.common.Util;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.HttpEntity;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class WebsiteBuilder extends Thread {
    private final WebsiteBuild build;

    protected WebsiteBuilder(WebsiteBuild build) {
        this.build = build;
    }

    @Override
    public void run() {
        Simofa.getLogger().info(String.format("Running build %s for website %d", build.getId(), build.getWebsite().getId()));
        build.setStatus(BuildStatus.PREPARING);

        try {
            File tmpDir = Util.createTempDir(build.getId());
            File tmpInDir = new File(tmpDir, "in");

            File tmpCacheDir = new File(tmpDir, "cache");
            File websiteCacheDir = new File(build.getCacheDir(), "website-" + build.getWebsite().getId());

            GitFetcher.fetch(build, tmpInDir, websiteCacheDir);

            File tmpScriptDir = new File(tmpDir, "scripts");
            FileOutputStream fileOutputStream = new FileOutputStream(new File(tmpScriptDir, "build.sh"));
            fileOutputStream.write(build.getWebsite().getBuildCommand().getBytes());
            fileOutputStream.close();

            File websiteCache = new File(websiteCacheDir, "cache.zip");
            if (websiteCache.exists()) {
                FileInputStream fileInputStream = new FileInputStream(websiteCache);
                FileOutputStream cacheFileOutputStream = new FileOutputStream(new File(tmpCacheDir, "cache.zip"));
                IOUtils.copy(fileInputStream, cacheFileOutputStream);
                fileInputStream.close();
                cacheFileOutputStream.close();

                build.addLog(new SimofaLog(LogType.INFO, "Using build cache"));
            }

            DockerCallback dockerCallback = new DockerCallback() {
                @Override
                public void created(String containerId) {
                    Simofa.getLogger().info("Container ID: " + containerId);
                    build.setContainerId(containerId);
                }

                private String createDeploymentServerUrl(String url) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(url);

                    if (!url.endsWith("/")) {
                        stringBuilder.append("/");
                    }

                    stringBuilder.append("deploy");

                    return stringBuilder.toString();
                }

                @Override
                void onExit(WaitResponse waitResponse) {
                    String exitMsg = String.format("Build completed with exit code %d", waitResponse.getStatusCode());

                    if (waitResponse.getStatusCode() == 0) {
                        build.addLog(new SimofaLog(LogType.INFO, exitMsg));
                        build.setStatus(BuildStatus.DEPLOYING);
                    } else {
                        build.addLog(new SimofaLog(LogType.ERROR, exitMsg));
                        if (!build.getStatus().equals(BuildStatus.STOPPED.toString())) {
                            build.setStatus(BuildStatus.ERROR);
                        }
                    }

                    Simofa.getBuildQueueManager().getBuildQueue().getCurrentBuilds()
                            .removeIf(w -> w.getId().equals(build.getId()));

                    if (waitResponse.getStatusCode() != 0) {
                        Simofa.getBuildQueueManager().getBuildQueue().remove(build);
                    } else {
                        File tempDir = null;
                        try {
                            tempDir = Util.createTempDir(build.getId() + "-dist");
                            Simofa.getDockerManager().copySiteZip(build, tempDir);

                            try {
                                String cacheDir = build.getCacheDir();
                                File outputDir = new File(cacheDir, "website-" + build.getWebsite().getId());

                                if (!Util.isBlank(cacheDir) && !outputDir.exists() && !outputDir.mkdir()) {
                                    throw new IOException("Failed to create output directory for cache");
                                }

                                Simofa.getDockerManager().copyCacheZip(build, outputDir);
                            } catch (Exception e) {
                                // Caching failing shouldn't fail the whole build, so try-catch here
                                build.addLog(new SimofaLog(LogType.WARN, e.getMessage()));
                            }

                            Optional<DeploymentServer> deploymentServer = BuildQueueManager.getDatabase()
                                    .getDeploymentServerDatabase().getDeploymentServer(
                                            build.getWebsite().getDeploymentServer()
                                    );

                            if (deploymentServer.isEmpty()) {
                                throw new Exception("Deployment server not found");
                            }

                            String siteHash = "";
                            try (InputStream siteFileReadBack = new FileInputStream(new File(tempDir, "site.zip"))) {
                                siteHash = DigestUtils.md5Hex(siteFileReadBack);
                            }

                            HttpPost request = new HttpPost(createDeploymentServerUrl(deploymentServer.get().getUrl()));

                            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                            builder.addTextBody("key", deploymentServer.get().getKey());
                            builder.addTextBody("site_hash", siteHash);
                            builder.addTextBody("build_url", String.format(
                                    "%s/api/websites/%d/build/%s",
                                    BuildQueueManager.getConfig().getUrl(),
                                    build.getWebsite().getId(),
                                    build.getId()
                            ));
                            builder.addTextBody("deployment_command", Util.base64Encode(build.getWebsite().getDeploymentCommand()));
                            builder.addTextBody("deployment_failed_command", Util.base64Encode(build.getWebsite().getDeploymentFailedCommand()));
                            builder.addTextBody("build_id", build.getId());
                            builder.addBinaryBody("site", new File(tempDir, "site.zip"));

                            HttpEntity entity = builder.build();
                            request.setEntity(entity);

                            try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
                                File finalTempDir = tempDir;
                                client.execute(request, response -> {
                                    int statusCode = response.getCode();
                                    if (statusCode != 200) {
                                        String httpResponse = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
                                        Simofa.getLogger().error(String.format("Received non-200 response from deploy server: %d %s", statusCode, httpResponse));
                                        build.addLog(new SimofaLog(
                                                LogType.ERROR,
                                                String.format("Received non-200 response from deploy server: %d %s", statusCode, httpResponse)
                                        ));
                                        build.setStatus(BuildStatus.ERROR);
                                    }

                                    try {
                                        FileUtils.deleteDirectory(finalTempDir);
                                    } catch (IOException e) {
                                        Simofa.getLogger().warn("Failed to delete temporary directory " + finalTempDir.getAbsolutePath(), e);
                                    } finally {
                                        Simofa.getDockerManager().deleteContainer(build.getContainerId());
                                    }
                                    return null;
                                });
                            }
                        } catch (Exception e) {
                            Simofa.getLogger().info("Failed to deploy site", e);
                            build.addLog(new SimofaLog(LogType.ERROR, "Failed to deploy site: " + e.getMessage()));
                            build.setStatus(BuildStatus.ERROR);
                            Simofa.getDockerManager().deleteContainer(build.getContainerId());

                            if (tempDir != null) {
                                try {
                                    FileUtils.deleteDirectory(tempDir);
                                } catch (IOException ex) {
                                    Simofa.getLogger().warn("Failed to delete temporary directory " + tempDir.getAbsolutePath(), ex);
                                }
                            }
                        }
                    }
                }

                @Override
                public void onStart(Closeable closeable) {}

                @Override
                public void onNext(Frame frame) {
                    LogType logType = null;
                    if (frame.getStreamType().equals(StreamType.STDOUT)) {
                        logType = LogType.INFO;
                    } else if (frame.getStreamType().equals(StreamType.STDERR)) {
                        logType = LogType.ERROR;
                    } else {
                        return;
                    }

                    build.addLog(new SimofaLog(logType, new String(frame.getPayload())));
                }

                @Override
                public void onError(Throwable throwable) {
                    throwable.printStackTrace();
                    build.addLog(new SimofaLog(LogType.ERROR, throwable.getMessage()));
                    build.setStatus(BuildStatus.ERROR);
                }

                @Override
                public void onComplete() {}

                @Override
                public void close() {}
            };

            // With the correct timing, it is possible to start a Docker container
            // for a build that was stopped while the files were being pulled from Git.
            // This fixes that edge case, and, as a precaution, prevents the container
            // from starting if a build is in an error state.
            if (build.getStatus().equals(BuildStatus.STOPPED.toString()) ||
                    build.getStatus().equals(BuildStatus.ERROR.toString())) {
                return;
            }

            build.setStatus(BuildStatus.BUILDING);

            Simofa.getDockerManager().createContainer(
                    build.getWebsite(),
                    dockerCallback,
                    tmpDir.getAbsolutePath()
            );
        } catch (Exception e) {
            Simofa.getLogger().error("Failed to build website", e);
            build.addLog(new SimofaLog(LogType.ERROR, e.getMessage()));
            build.addLog(new SimofaLog(LogType.ERROR, "Build failed: Error in " + e.getClass().getName()));
            build.setStatus(BuildStatus.ERROR);
        } finally {
            String tmpDirPath = Util.getTempDirPath(build.getId());
            Simofa.getLogger().info(String.format("Deleting temporary directory for %s", build.getId()));
            try {
                FileUtils.deleteDirectory(new File(tmpDirPath));
            } catch (Exception e) {
                e.printStackTrace();
                Simofa.getLogger().error("Failed to delete temporary directory " + tmpDirPath, e);
            }
        }
    }
}
