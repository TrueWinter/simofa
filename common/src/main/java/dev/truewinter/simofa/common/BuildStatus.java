package dev.truewinter.simofa.common;

public enum BuildStatus {
    QUEUED,
    PREPARING,
    BUILDING,
    DEPLOYING,
    DEPLOYED,
    ERROR,
    STOPPED;
}
