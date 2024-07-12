package dev.truewinter.simofa.api;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class DeploymentServer {
    private final String id;
    private final String name;
    private final String url;
    @JsonIgnore
    private final String key;

    public DeploymentServer(String id, String name, String url, String key) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.key = key;
    }

    public String getId() {
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
