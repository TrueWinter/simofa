package dev.truewinter.simofa.common;

import org.jetbrains.annotations.Nullable;

public enum BuildStatus {
    QUEUED("queued"),
    BUILDING("building"),
    DEPLOYING("deploying"),
    DEPLOYED("deployed"),
    ERROR("error"),
    STOPPED("stopped");

    private final String value;
    BuildStatus(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Nullable
    public static BuildStatus toBuildStatus(String status) {
        for (BuildStatus b : BuildStatus.values()) {
            if (b.toString().equals(status)) {
                return b;
            }
        }

        return null;
    }
}
