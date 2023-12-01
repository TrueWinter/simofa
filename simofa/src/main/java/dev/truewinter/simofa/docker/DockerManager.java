package dev.truewinter.simofa.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.core.RemoteApiVersion;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import dev.truewinter.simofa.Simofa;
import dev.truewinter.simofa.Website;
import dev.truewinter.simofa.config.Config;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DockerManager {
    private static DockerManager dockerManager;
    private final DockerClient dockerClient;

    private DockerManager(Config config) {
        DefaultDockerClientConfig.Builder clientConfigBuilder = DefaultDockerClientConfig
                .createDefaultConfigBuilder()
                .withApiVersion(RemoteApiVersion.VERSION_1_40)
                .withDockerTlsVerify(config.shouldDockerVerifyTls())
                .withDockerHost(config.getDockerHost());

        if (!config.getDockerCertPath().isBlank()) {
            clientConfigBuilder.withDockerCertPath(config.getDockerCertPath());
        }

        if (!config.getDockerUrl().isBlank()) {
            clientConfigBuilder.withRegistryUrl(config.getDockerUrl())
                    .withRegistryUsername(config.getDockerUsername())
                    .withRegistryPassword(config.getDockerPassword())
                    .withRegistryEmail(config.getDockerEmail());
        }

        DockerClientConfig clientConfig = clientConfigBuilder.build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(clientConfig.getDockerHost())
                .sslConfig(clientConfig.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                //.responseTimeout(Duration.ofSeconds(45))
                .build();

        dockerClient = DockerClientImpl.getInstance(clientConfig, httpClient);

        List<dev.truewinter.simofa.docker.Image> imageList = getImages();
        if (imageList.isEmpty()) {
            Simofa.getLogger().error("No Docker images available");
        } else {
            Simofa.getLogger().info("Docker images found:");
            imageList.forEach(i -> {
                Simofa.getLogger().info(String.format("\t- %s (%s)",
                        String.join(",", i.getName()),
                        i.getSize())
                );
            });
        }

        List<dev.truewinter.simofa.docker.Container> containerList = getContainers();
        if (!containerList.isEmpty()) {
            Simofa.getLogger().warn("Containers found. Simofa expects to be the only software managing Docker containers on the server it's installed on.");
            containerList.forEach(c -> {
                Simofa.getLogger().warn(String.format("\t- %s (%s)", c.getName(), c.getId()));
            });
        }
    }

    public static DockerManager getInstance(Config config) {
        if (dockerManager == null) {
            dockerManager = new DockerManager(config);
        }
        return dockerManager;
    }

    public void shutdown() throws IOException {
        Simofa.getLogger().info("Removing running containers");
        getContainers().forEach(c -> {
            Simofa.getLogger().info(String.format("Removing container %s", c.getId()));
            try {
                deleteContainer(c.getId());
            } catch (Exception e) {
                Simofa.getLogger().error("Failed to shut down container", e);
            }
        });
        dockerClient.close();
    }

    public List<dev.truewinter.simofa.docker.Image> getImages() {
        List<com.github.dockerjava.api.model.Image> imageList =  dockerClient.listImagesCmd().exec();
        List<dev.truewinter.simofa.docker.Image> images = new ArrayList<>();
        imageList.forEach(i -> {
            for (String img : i.getRepoTags()) {
                if (!img.toLowerCase().startsWith("simofa-")) continue;
                images.add(new dev.truewinter.simofa.docker.Image(
                        img,
                        String.format("%dM", i.getSize() / 1000 / 1000)
                ));
            }
        });

        return images;
    }

    public List<dev.truewinter.simofa.docker.Container> getContainers() {
        List<com.github.dockerjava.api.model.Container> containerList = dockerClient.listContainersCmd().withShowAll(true).exec();
        List<dev.truewinter.simofa.docker.Container> containers = new ArrayList<>();
        containerList.forEach(c -> {
            containers.add(new dev.truewinter.simofa.docker.Container(
                    c.getId(),
                    Arrays.stream(c.getNames()).findFirst().orElse("<unknown>"),
                    c.getState(),
                    c.getStatus()
            ));
        });

        return containers;
    }

    public void deleteContainer(String id) {
        dockerClient.removeContainerCmd(id).withForce(true).withRemoveVolumes(true).exec();
    }

    @SuppressWarnings("RedundantThrows")
    public void createContainer(Website website, DockerCallback dockerCallback, String repoDir) throws Exception {
        final long CPU_PERIOD = 100000L;
        final double CPUS = website.getCpu();

        HostConfig hostConfig = HostConfig.newHostConfig()
                .withCpuPeriod(CPU_PERIOD)
                .withCpuQuota((long) (CPU_PERIOD * CPUS))
                .withMemory(website.getMemory() * 1000 * 1000L);

        String buildScript = "/simofa/scripts/build.sh";

        CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(website.getDockerImage())
                .withHostConfig(hostConfig)
                // -l is required for the source command to work
                // which is required for some software
                // (such as bundler) to work
                .withEntrypoint("/bin/bash", "-l", "-c")
                .withCmd("dos2unix {script} && chmod +x {script} && {script}".replace("{script}", buildScript));
        CreateContainerResponse createContainerResponse = createContainerCmd.exec();
        String id = createContainerResponse.getId();
        dockerCallback.created(id);

        dockerClient.copyArchiveToContainerCmd(id)
                .withHostResource(repoDir)
                .withRemotePath("/simofa")
                .withDirChildrenOnly(true)
                .exec();

        StartContainerCmd startContainerCmd = dockerClient.startContainerCmd(id);
        startContainerCmd.exec();

        LogContainerCmd logContainerCmd = dockerClient.logContainerCmd(id)
                .withStdOut(true)
                .withStdErr(true)
                .withFollowStream(true);

        logContainerCmd.exec(dockerCallback);

        dockerClient.waitContainerCmd(id)
                .exec(new DockerWaitCallback() {
                    @Override
                    public void onExit(WaitResponse waitResponse) {
                        dockerCallback.onExit(waitResponse);
                    }
                });
    }

    public void copySiteZip(WebsiteBuild websiteBuild, File destinationDir) throws Exception {
        if (websiteBuild.getContainerId() == null) {
            throw new Exception("Container does not exist");
        }

        try (TarArchiveInputStream inputStream = new TarArchiveInputStream(dockerClient.copyArchiveFromContainerCmd(
                websiteBuild.getContainerId(),
                "/simofa/out/site.zip"
        ).exec())) {
                unTar(inputStream, new File(destinationDir, "site.zip"));
        }
    }

    public void copyCacheZip(WebsiteBuild websiteBuild, File destinationDir) throws Exception {
        if (websiteBuild.getContainerId() == null) {
            throw new Exception("Container does not exist");
        }

        File outputFile = new File(destinationDir, "cache.zip");
        if (outputFile.exists() && !outputFile.delete()) {
            throw new IOException("Failed to delete cache file");
        }

        try (TarArchiveInputStream inputStream = new TarArchiveInputStream(dockerClient.copyArchiveFromContainerCmd(
                websiteBuild.getContainerId(),
                "/simofa/cache/cache.zip"
        ).exec())) {
            unTar(inputStream, new File(destinationDir, "cache.zip"));
        } catch (FileNotFoundException ignored) {} // Cache is optional
    }

    // https://github.com/docker-java/docker-java/issues/991
    private void unTar(TarArchiveInputStream tis, File destFile) throws IOException {
        TarArchiveEntry tarEntry = null;
        while ((tarEntry = tis.getNextTarEntry()) != null) {
            if (tarEntry.isDirectory()) {
                if (!destFile.exists()) {
                    if (!destFile.mkdirs()) {
                        throw new IOException("Failed to create directory");
                    }
                }
            } else {
                FileOutputStream fos = new FileOutputStream(destFile);
                IOUtils.copy(tis, fos);
                fos.close();
            }
        }
        tis.close();
    }

}
