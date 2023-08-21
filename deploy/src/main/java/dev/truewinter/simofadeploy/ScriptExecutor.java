package dev.truewinter.simofadeploy;

import dev.truewinter.simofa.common.LogType;
import dev.truewinter.simofa.common.SimofaLog;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class ScriptExecutor {
    private static final String shell = "/bin/bash -c";
    private static final long FIVE_MINUTES = 5 * 60 * 1000;

    public static void run(WebsiteDeployment websiteDeployment, String scriptName, ScriptExecutionCallback callback) {
        final long startTime = System.currentTimeMillis();

        File script = new File(websiteDeployment.getTmpScriptsDir(), scriptName);
        Process process;
        try {
            // TODO: Find way to prevent this from being instantly killed
            //  by Ctrl+C, so that WebsiteDeployer#stopAndRollback() can
            //  gracefully kill the process and submit the final logs.
            process = Runtime.getRuntime().exec(
                    String.format("%s %s", shell, script.getAbsolutePath()),
                    null, websiteDeployment.getTmpInDir()
            );
        } catch (Exception e) {
            callback.onError(e);
            return;
        }

        callback.onStart(process);

        new StreamReader(process.getInputStream(), LogType.INFO, new StreamReader.StreamReaderCallback() {
            @Override
            public void onLog(SimofaLog log) {
                websiteDeployment.addLog(log);
            }
        }).start();

        new StreamReader(process.getErrorStream(), LogType.ERROR, new StreamReader.StreamReaderCallback() {
            @Override
            public void onLog(SimofaLog log) {
                websiteDeployment.addLog(log);
            }
        }).start();

        // While process.waitFor() would be better in most cases,
        // we need the ability to stop the Process from outside
        // the ScriptExecutor at any time. waitFor() blocks the thread,
        // so by the time the Process is returned to the caller of the run()
        // method, the Process is already done.
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    int exitCode = process.exitValue();
                    timer.cancel();
                    if (exitCode != 0) {
                        callback.onError(new Exception("Exited with non-zero error code " + exitCode));
                    } else {
                        callback.onExit();
                    }
                } catch (IllegalThreadStateException ignored) {
                    if (System.currentTimeMillis() - startTime >= FIVE_MINUTES) {
                        try {
                            if (!process.destroyForcibly().waitFor(30, TimeUnit.SECONDS)) {
                                websiteDeployment.addLog(new SimofaLog(LogType.ERROR, "Failed to stop script after 30 seconds"));
                            }
                        } catch (InterruptedException ignored1) {}
                        callback.onError(new Exception("Script took longer than 5 minutes to complete"));
                    }
                }
            }
        }, 0, 10);

        /*if (process.waitFor(5, TimeUnit.MINUTES)) {
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new Exception("Exited with non-zero error code " + exitCode);
            }
        } else {
            if (!process.destroyForcibly().waitFor(30, TimeUnit.SECONDS)) {
                websiteDeployment.addLog(new SimofaLog(LogType.ERROR, "Failed to stop script after 30 seconds"));
            }
            throw new Exception("Script took longer than 5 minutes to complete");
        }*/
    }

    public abstract static class ScriptExecutionCallback {
        abstract void onStart(Process process);
        abstract void onExit();
        abstract void onError(Exception e);
    }
}
