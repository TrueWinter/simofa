package dev.truewinter.simofa.api;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class DeploymentServer {
    private final int id;
    private final String name;
    private final String url;
    @JsonIgnore
    private final String key;

    public DeploymentServer(int id, String name, String url, String key) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.key = key;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getKey() {
        return key;
    }
}
