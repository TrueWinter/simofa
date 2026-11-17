package dev.truewinter.simofa.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.truewinter.simofa.api.events.BuildQueuedEvent;
import dev.truewinter.simofa.api.events.BuildStatusChangedEvent;
import dev.truewinter.simofa.api.internal.WsRegistry;
import dev.truewinter.simofa.common.BuildStatus;
import dev.truewinter.simofa.common.LogType;
import dev.truewinter.simofa.common.SimofaLog;
import dev.truewinter.simofa.common.Util;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private long queuedAt;
    @JsonIgnore
    private long startTime;
    @JsonIgnore
    private long endTime;
    @JsonIgnore
    private final String cacheDir;
    @JsonIgnore
    private PersistenceListener persistenceListener;

    public static final Set<BuildStatus> END_STATUSES = new HashSet<>(){{
        add(BuildStatus.STOPPED);
        add(BuildStatus.ERROR);
        add(BuildStatus.DEPLOYED);
    }};

    public interface PersistenceListener {
        void onStatusChanged(WebsiteBuild build);
        void onLogAdded(WebsiteBuild build, SimofaLog log);
    }

    public WebsiteBuild(Website website, String commit, String cacheDir) {
        this(Util.createv7UUID().toString(), website, commit, BuildStatus.QUEUED, new ArrayList<>(),
                System.currentTimeMillis(), 0, 0, null, cacheDir);

        addLog(new SimofaLog(LogType.INFO, "Build queued"));

        SimofaPluginManager.getInstance().getPluginManager().fireEvent(new BuildQueuedEvent(this));
        WsRegistry.accept(WsRegistry.Instances.BUILD_QUEUE, website.getId(), null);
    }

    private WebsiteBuild(String id, Website website, String commit, BuildStatus status,
                         ArrayList<SimofaLog> logs, long queuedAt, long startTime, long endTime,
                         String containerId, String cacheDir) {
        this.id = id;
        this.website = website;
        this.commit = commit;
        this.status = status;
        this.logs = logs;
        this.queuedAt = queuedAt;
        this.startTime = startTime;
        this.endTime = endTime;
        this.containerId = containerId;
        this.cacheDir = cacheDir;
    }

    public static WebsiteBuild fromDatabase(String id, Website website, String commit, BuildStatus status,
                                            long queuedAt, long startTime, long endTime,
                                            List<SimofaLog> logs, String cacheDir) {
        return new WebsiteBuild(id, website, commit, status,
                logs == null ? new ArrayList<>() : new ArrayList<>(logs),
                queuedAt, startTime, endTime, null, cacheDir);
    }

    public void restoreAsError(String message) {
        if (END_STATUSES.contains(this.status)) return;

        this.status = BuildStatus.ERROR;
        this.endTime = System.currentTimeMillis();

        addLog(new SimofaLog(LogType.ERROR, message));

        if (persistenceListener != null) {
            persistenceListener.onStatusChanged(this);
        }
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

        if (!END_STATUSES.contains(this.status) && END_STATUSES.contains(status)) {
            endTime = System.currentTimeMillis();
        }

        this.status = status;

        if (persistenceListener != null) {
            persistenceListener.onStatusChanged(this);
        }

        addLog(new SimofaLog(LogType.INFO, "Build status changed to " + status.toString().toLowerCase()));
        SimofaPluginManager.getInstance().getPluginManager().fireEvent(new BuildStatusChangedEvent(this));
        WsRegistry.accept(WsRegistry.Instances.BUILD_QUEUE, website.getId(), null);
    }

    @Nullable
    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public void setPersistenceListener(@Nullable PersistenceListener persistenceListener) {
        this.persistenceListener = persistenceListener;
    }

    public long getQueuedAt() {
        return queuedAt;
    }

    public ArrayList<SimofaLog> getLogs() {
        return logs;
    }

    public void addLog(SimofaLog log) {
        logs.add(log);

        if (persistenceListener != null) {
            persistenceListener.onLogAdded(this, log);
        }

        WsRegistry.accept(WsRegistry.Instances.WEBSITE_LOGS, this, log);
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
    public File getCacheDir() {
        if (cacheDir == null) return null;
        return new File(cacheDir, "website-" + getWebsite().getId());
    }

    public boolean useCache() {
        return !commit.toLowerCase().contains("[no cache]");
    }
}
