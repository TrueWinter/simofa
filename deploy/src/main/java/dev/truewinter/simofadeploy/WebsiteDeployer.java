package dev.truewinter.simofadeploy;

import dev.truewinter.simofa.common.BuildStatus;
import dev.truewinter.simofa.common.LogType;
import dev.truewinter.simofa.common.SimofaLog;
import org.apache.commons.io.FileUtils;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

public class WebsiteDeployer extends Thread {
    private final WebsiteDeployment websiteDeployment;
    //private final LogSubmitter logSubmitter;
    //private final StatusSubmitter statusSubmitter;
    private final WsSubmitter submitter;
    private boolean submitting = false;
    private boolean errored = false;
    private Process deployProcess = null;
    private Timer timer = null;

    public WebsiteDeployer(WebsiteDeployment websiteDeployment) {
        this.websiteDeployment = websiteDeployment;

        /*BuildServer buildServer = new BuildServer(
                websiteDeployment.getBuildUrl(),
                websiteDeployment.getKey()
        );*/

        this.submitter = new WsSubmitter(websiteDeployment);
        //this.logSubmitter = new LogSubmitter(buildServer);
        //this.statusSubmitter = new StatusSubmitter(buildServer);
    }

    private void rollback(Exception e, ScriptExecutor.ScriptExecutionCallback callback) {
        // Only roll back once, no matter how many submitters throw an exception
        if (errored) return;
        errored = true;

        SimofaDeploy.getLogger().warn("Rolling back deployment due to error", e);
        websiteDeployment.addLog(new SimofaLog(LogType.ERROR, "An error occurred while deploying: " + e.getMessage()));

        websiteDeployment.addLog(new SimofaLog(LogType.INFO, "Rolling back deployment"));
        websiteDeployment.setBuildStatus(BuildStatus.ERROR);

        if (deployProcess != null && deployProcess.isAlive()) {
            try {
                deployProcess.destroyForcibly().waitFor(30, TimeUnit.SECONDS);
            } catch (Exception ex) {
                callback.onError(new Exception("Failed to roll back deployment " + websiteDeployment.getBuildId(), ex));
                return;
            }
        }

        ScriptExecutor.run(websiteDeployment, WebsiteDeployment.DEPLOY_FAILED_CMD_NAME, callback);
    }

    @Override
    public void run() {
        /*timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (submitting) {
                    SimofaDeploy.getLogger().info("Previous log/status submission run not finished yet, queueing logs");
                    return;
                }
                submitting = true;

                ConcurrentSubmitterConfig<LogSubmitter, List<SimofaLog>> logSubmitterConfig = new ConcurrentSubmitterConfig<>(
                        logSubmitter,
                        websiteDeployment.getQueuedLogs()
                );

                ConcurrentSubmitterConfig<StatusSubmitter, BuildStatus> statusSubmitterConfig = new ConcurrentSubmitterConfig<>(
                        statusSubmitter,
                        websiteDeployment.getStatus()
                );

                //noinspection unchecked
                ConcurrentSubmitter.run(
                        new ConcurrentSubmitterConfig[]{
                                logSubmitterConfig,
                                statusSubmitterConfig
                        },
                        new Submitter.SubmitterCallback() {
                            @Override
                            public void done() {
                                submitting = false;
                            }

                            @Override
                            public void error(Exception e) {
                                timer.cancel();
                                rollback(e, new ScriptExecutor.ScriptExecutionCallback() {
                                    @Override
                                    void onStart(Process process) {}

                                    @Override
                                    void onExit() {
                                        cleanup();
                                    }

                                    @Override
                                    void onError(Exception e) {
                                        SimofaDeploy.getLogger().error("An error occurred while rolling back deployment", e);
                                        websiteDeployment.addLog(new SimofaLog(LogType.ERROR, "An error occurred while rolling back deployment: " + e.getMessage()));
                                        cleanup();
                                    }

                                    /*private void done() {
                                        if (websiteDeployment.getTmpDir().exists()) {
                                            try {
                                                FileUtils.deleteDirectory(websiteDeployment.getTmpDir());
                                            } catch (Exception e) {
                                                SimofaDeploy.getLogger().warn("Failed to delete temporary directory", e);
                                            }
                                        }

                                        SimofaDeploy.getDeployQueueManager().getDeployQueue().done(websiteDeployment);
                                        logSubmitter.submit(websiteDeployment.getQueuedLogs(), new Submitter.NoopSubmitterCallback());
                                        statusSubmitter.submit(websiteDeployment.getStatus(), new Submitter.NoopSubmitterCallback());
                                    }*\/
                                });
                            }
                        }
                );
            }
        }, 5000, 5000);*/

        ScriptExecutor.run(websiteDeployment, WebsiteDeployment.DEPLOY_CMD_NAME, new ScriptExecutor.ScriptExecutionCallback() {
            @Override
            void onStart(Process process) {
                deployProcess = process;
            }

            @Override
            void onExit() {
                websiteDeployment.addLog(new SimofaLog(LogType.INFO, "Deployed successfully"));
                websiteDeployment.setBuildStatus(BuildStatus.DEPLOYED);
                cleanup();
            }

            @Override
            void onError(Exception e) {
                rollback(e, new ScriptExecutor.ScriptExecutionCallback() {
                    @Override
                    void onStart(Process process) {}

                    @Override
                    void onExit() {
                        SimofaDeploy.getLogger().info(String.format("Rolled back deployment %s", websiteDeployment.getBuildId()));
                        websiteDeployment.addLog(new SimofaLog(LogType.INFO, "Rolled back deployment"));
                        cleanup();
                    }

                    @Override
                    void onError(Exception e) {
                        SimofaDeploy.getLogger().error("An error occurred while rolling back deployment", e);
                        websiteDeployment.addLog(new SimofaLog(LogType.ERROR, "An error occurred while rolling back deployment: " + e.getMessage()));
                        cleanup();
                    }
                });
            }
        });
    }

    private void cleanup() {
        submitter.disconnect();
        deployProcess = null;
        SimofaDeploy.getDeployQueueManager().getDeployQueue().done(websiteDeployment);
        if (timer != null) {
            timer.cancel();
        }
        //logSubmitter.submit(websiteDeployment.getQueuedLogs(), new Submitter.NoopSubmitterCallback());
        //statusSubmitter.submit(websiteDeployment.getStatus(), new Submitter.NoopSubmitterCallback());

        if (websiteDeployment.getTmpDir().exists()) {
            try {
                FileUtils.deleteDirectory(websiteDeployment.getTmpDir());
            } catch (Exception e) {
                SimofaDeploy.getLogger().warn("Failed to delete temporary directory", e);
            }
        }
    }

    public WebsiteDeployment getWebsiteDeployment() {
        return websiteDeployment;
    }

    // See comment in ScriptExecutor
    /*public void stopAndRollBack() {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        rollback(new Exception("Simofa Deploy shutting down"), new ScriptExecutor.ScriptExecutionCallback() {
            @Override
            void onStart(Process process) {}

            @Override
            void onExit() {
                done();
            }

            @Override
            void onError(Exception e) {
                done();
            }

            private void done() {
                countDownLatch.countDown();
            }
        });

        try {
            if (!countDownLatch.await(5, TimeUnit.MINUTES)) {
                throw new Exception("Awaiting rollback took too long");
            }
        } catch (Exception e) {
            SimofaDeploy.getLogger().error("Failed to await rollback", e);
        }
    }*/
}
