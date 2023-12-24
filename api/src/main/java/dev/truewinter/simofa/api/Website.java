package dev.truewinter.simofa.api;

import dev.truewinter.simofa.common.Util;
import org.jetbrains.annotations.Nullable;

public class Website {
    private final int id;
    private final String name;
    private final String dockerImage;
    private final int memory;
    private final double cpu;
    private final String gitUrl;
    private final String gitBranch;
    private final GitCredential gitCredential;
    private final String buildCommand;
    private final String deploymentCommand;
    private final String deploymentFailedCommand;
    private final int deploymentServer;
    private final String deployToken;

    public Website(int id, String name, String dockerImage, int memory,
                   double cpu, String gitUrl, String gitBranch, GitCredential gitCredential,
                   String buildCommand, String deploymentCommand, String deploymentFailedCommand,
                   int deploymentServer, String deployToken) {
        this.id = id;
        this.name = name;
        this.dockerImage = dockerImage;
        this.memory = memory;
        this.cpu = cpu;
        this.gitUrl = gitUrl;
        this.gitBranch = gitBranch;
        this.gitCredential = gitCredential;
        this.buildCommand = Util.dos2unix(buildCommand);
        this.deploymentCommand = Util.dos2unix(deploymentCommand);
        this.deploymentFailedCommand = Util.dos2unix(deploymentFailedCommand);
        this.deploymentServer = deploymentServer;
        this.deployToken = deployToken;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDockerImage() {
        return dockerImage;
    }

    public int getMemory() {
        return memory;
    }

    public double getCpu() {
        return cpu;
    }

    public String getGitUrl() {
        return gitUrl;
    }

    public String getGitBranch() {
        return gitBranch;
    }

    @Nullable
    public GitCredential getGitCredential() {
        return gitCredential;
    }

    public String getBuildCommand() {
        return buildCommand;
    }

    public String getDeploymentCommand() {
        return deploymentCommand;
    }

    public String getDeploymentFailedCommand() {
        return deploymentFailedCommand;
    }

    public int getDeploymentServer() {
        return deploymentServer;
    }

    public String getDeployToken() {
        return deployToken;
    }
}
