package dev.truewinter.simofa.docker;

public class Container {
    private final String id;
    private final String name;
    private final String state;
    private final String status;

    public Container(String id, String name, String state, String status) {
        this.id = id;
        this.name = name.replaceFirst("^/", "");
        this.state = state;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getState() {
        return state;
    }

    public String getStatus() {
        return status;
    }
}
