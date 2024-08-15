package dev.truewinter.simofa.api;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class GitCredential {
    private String id;
    private String username;
    @JsonIgnore
    private String password;

    public GitCredential() {}

    public GitCredential(String id, String username, String password) {
        this.id = id;
        this.username = username;
        this.password = password;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
