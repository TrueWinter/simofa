package dev.truewinter.simofa;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class GitCredential {
    private final int id;
    private final String username;
    @JsonIgnore
    private final String password;

    public GitCredential(int id, String username, String password) {
        this.id = id;
        this.username = username;
        this.password = password;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
