package dev.truewinter.simofa.docker;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.truewinter.simofa.Website;
import dev.truewinter.simofa.common.BuildStatus;
import dev.truewinter.simofa.common.SimofaLog;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.UUID;

public class WebsiteBuild {
    private final String id;
    private final Website website;
    private final String commit;
    private BuildStatus status;
    @JsonIgnore
    private final ArrayList<SimofaLog> logs;
    @JsonIgnore
    private String containerId;
    @JsonIgnore
    private long startTime;
    @JsonIgnore
    private long endTime;

    protected WebsiteBuild(Website website, String commit) {
        this.id = UUID.randomUUID().toString();
        this.website = website;
        this.commit = commit;
        this.status = BuildStatus.QUEUED;
        this.logs = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public Website getWebsite() {
        return website;
    }

    public String getCommit() {
        return commit;
    }

    public String getStatus() {
        return status.toString();
    }

    public void setStatus(BuildStatus status) {
        if (status.equals(BuildStatus.BUILDING)) {
            startTime = System.currentTimeMillis();
        }

        if (!(this.status.equals(BuildStatus.STOPPED) || this.status.equals(BuildStatus.ERROR) ||
                this.status.equals(BuildStatus.DEPLOYED)) &&
                (status.equals(BuildStatus.STOPPED) || status.equals(BuildStatus.ERROR) ||
                        status.equals(BuildStatus.DEPLOYED))) {
            endTime = System.currentTimeMillis();
        }

        this.status = status;
    }

    @Nullable
    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public ArrayList<SimofaLog> getLogs() {
        return logs;
    }

    public void addLog(SimofaLog log) {
        logs.add(log);
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public long getRunTime() {
        if (startTime == 0L) {
            return 0;
        }

        long endOrCurrent = endTime;

        if (endTime == 0L) {
            endOrCurrent = System.currentTimeMillis();
        }

        return endOrCurrent - startTime;
    }

}
