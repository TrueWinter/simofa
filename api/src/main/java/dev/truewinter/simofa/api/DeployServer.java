package dev.truewinter.simofa.api;

public class DeployServer {
    private String id;
    private String name;
    private String url;
    private String key;

    public DeployServer() {}

    public DeployServer(String id, String name, String url, String key) {
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
