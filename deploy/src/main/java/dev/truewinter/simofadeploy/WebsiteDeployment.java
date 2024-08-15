package dev.truewinter.simofadeploy;

import dev.truewinter.simofa.common.BuildStatus;
import dev.truewinter.simofa.common.SimofaLog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class WebsiteDeployment {
    public static String SITE_ZIP_NAME = "site.zip";
    public static String DEPLOY_CMD_NAME = "deploy.sh";
    public static String DEPLOY_FAILED_CMD_NAME = "undeploy.sh";

    private final String buildId;
    private final String buildUrl;
    private final String key;
    private final File tmpDir;
    private final List<SimofaLog> logs;
    //private final List<SimofaLog> queuedLogs;
    private WsSubmitter submitter;

    private BuildStatus status = BuildStatus.DEPLOYING;

    public WebsiteDeployment(String buildId, String buildUrl, String key, File tmpDir) {
        this.buildId = buildId;
        this.buildUrl = buildUrl;
        this.key = key;
        this.tmpDir = tmpDir;

        logs = new ArrayList<>();
        //queuedLogs = new ArrayList<>();
    }

    public String getBuildId() {
        return buildId;
    }

    public String getBuildUrl() {
        return buildUrl;
    }

    public String getKey() {
        return key;
    }

    public File getTmpDir() {
        return tmpDir;
    }

    public File getTmpInDir() {
        return new File(tmpDir, "in");
    }

    public File getTmpScriptsDir() {
        return new File(tmpDir, "scripts");
    }

    public synchronized List<SimofaLog> getLogs() {
        return logs;
    }

    /*public synchronized List<SimofaLog> getQueuedLogs() {
        List<SimofaLog> queuedLogsCopy = new ArrayList<>(queuedLogs);
        queuedLogs.clear();
        return queuedLogsCopy;
    }*/

    public synchronized void addLog(SimofaLog log) {
        logs.add(log);
        //queuedLogs.add(log);

        if (submitter != null) {
            submitter.submitLog(log);
        }
    }

    public void setBuildStatus(BuildStatus status) {
        this.status = status;

        if (submitter != null) {
            submitter.submitStatus(status);
        }
    }

    public BuildStatus getStatus() {
        return status;
    }

    public void setSubmitter(WsSubmitter submitter) {
        this.submitter = submitter;
    }
}
