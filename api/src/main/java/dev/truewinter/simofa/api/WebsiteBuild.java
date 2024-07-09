package dev.truewinter.simofa.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.truewinter.simofa.api.events.BuildQueuedEvent;
import dev.truewinter.simofa.api.events.BuildStatusChangedEvent;
import dev.truewinter.simofa.common.BuildStatus;
import dev.truewinter.simofa.common.LogType;
import dev.truewinter.simofa.common.SimofaLog;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

@SuppressWarnings("unused")
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
    @JsonIgnore
    private final String cacheDir;

    private static BiConsumer<WebsiteBuild, SimofaLog> sseConsumer;

    public WebsiteBuild(Website website, String commit, String cacheDir) {
        this.id = UUID.randomUUID().toString();
        this.website = website;
        this.commit = commit;
        this.status = BuildStatus.QUEUED;
        this.logs = new ArrayList<>();
        this.cacheDir = cacheDir;

        addLog(new SimofaLog(LogType.INFO, "Build queued"));

        SimofaPluginManager.getInstance().getPluginManager().fireEvent(new BuildQueuedEvent(this));
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

    public BuildStatus getStatus() {
        return status;
    }

    public void setStatus(BuildStatus status) {
        if (this.status.equals(BuildStatus.STOPPED)) return;

        if (status.equals(BuildStatus.BUILDING)) {
            startTime = System.currentTimeMillis();
        }

        Set<BuildStatus> endStatuses = new HashSet<>(){{
            add(BuildStatus.STOPPED);
            add(BuildStatus.ERROR);
            add(BuildStatus.DEPLOYED);
        }};

        if (!endStatuses.contains(this.status) && endStatuses.contains(status)) {
            endTime = System.currentTimeMillis();
        }

        this.status = status;

        addLog(new SimofaLog(LogType.INFO, "Build status changed to " + status.toString().toLowerCase()));

        SimofaPluginManager.getInstance().getPluginManager().fireEvent(new BuildStatusChangedEvent(this));
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
        if (sseConsumer != null) {
            sseConsumer.accept(this, log);
        }
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

    @Nullable
    public String getCacheDir() {
        if (commit.toLowerCase().contains("[no cache]")) {
            return null;
        }

        return cacheDir;
    }

    @ApiStatus.Internal
    public static void internal_registerSseConsumer(BiConsumer<WebsiteBuild, SimofaLog> consumer) {
        WebsiteBuild.sseConsumer = consumer;
    }
}
