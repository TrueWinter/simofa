package dev.truewinter.simofa.common;

public enum LogType {
    INFO("info"),
    WARN("warn"),
    ERROR("error");

    private final String type;

    LogType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }
}
