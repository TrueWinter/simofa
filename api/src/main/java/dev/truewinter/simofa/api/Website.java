package dev.truewinter.simofa.api;

import dev.truewinter.simofa.common.Util;
import org.jetbrains.annotations.Nullable;

public class Website {
    private String id;
    private String name;
    private String dockerImage;
    private int memory;
    private double cpu;
    private String gitUrl;
    private String gitBranch;
    private String gitCredentials;
    private BUILD_ON buildOn;
    private String buildCommand;
    private String deployCommand;
    private String deployFailedCommand;
    private String deployServer;
    private String deployToken;

    public Website() {}

    public Website(String id, String name, String dockerImage, int memory,
                   double cpu, String gitUrl, String gitBranch, String gitCredentials,
                   BUILD_ON buildOn, String buildCommand, String deployCommand,
                   String deployFailedCommand, String deployServer, String deployToken) {
        this.id = id;
        this.name = name;
        this.dockerImage = dockerImage;
        this.memory = memory;
        this.cpu = cpu;
        this.gitUrl = gitUrl;
        this.gitBranch = gitBranch;
        this.gitCredentials = gitCredentials;
        this.buildOn = buildOn;
        this.buildCommand = Util.dos2unix(buildCommand);
        this.deployCommand = Util.dos2unix(deployCommand);
        this.deployFailedCommand = Util.dos2unix(deployFailedCommand);
        this.deployServer = deployServer;
        this.deployToken = deployToken;
    }

    public String getId() {
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
    public String getGitCredentials() {
        return gitCredentials;
    }

    public BUILD_ON getBuildOn() {
        return buildOn;
    }

    public String getBuildCommand() {
        return buildCommand;
    }

    public String getDeployCommand() {
        return deployCommand;
    }

    public String getDeployFailedCommand() {
        return deployFailedCommand;
    }

    public String getDeployServer() {
        return deployServer;
    }

    public String getDeployToken() {
        return deployToken;
    }

    public enum BUILD_ON {
        COMMIT,
        TAG,
        RELEASE
    }
}
