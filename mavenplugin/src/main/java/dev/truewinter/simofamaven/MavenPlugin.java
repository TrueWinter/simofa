package dev.truewinter.simofamaven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
@Mojo(name = "build")
public class MavenPlugin extends AbstractMojo {
    @Parameter(property = "command", required = true)
    private String command;
    @Parameter(property = "envs")
    private ArrayList<String> envs = new ArrayList<>();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // https://www.baeldung.com/run-shell-command-in-java
        boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

        String shell = isWindows ? "cmd.exe /c" : "/bin/bash -c";
        try {
            String cmd = String.format(shell, command);
            StringBuilder envVars = new StringBuilder();
            for (String e : envs) {
                if (isWindows) {
                    envVars.append(String.format("set \"%s\" && ", e));
                } else {
                    envVars.append(String.format("export \"%s\" && ", e));
                }
            }
            //noinspection RedundantStringFormatCall
            System.out.println(String.format("%s %s %s", shell, envVars, command));
            Process process = Runtime.getRuntime().exec(String.format("%s %s %s", shell, envVars, command));

            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    System.out.println("Destroying process");
                    process.destroyForcibly();
                }
            });

            BufferedReader outReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            String outLine = "";
            while ((outLine = outReader.readLine()) != null) {
                System.out.println(outLine);
            }

            String errLine = "";
            while ((errLine = errReader.readLine()) != null) {
                System.out.println(errLine);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new Exception("Exited with non-zero error code");
            }
            System.out.println("Ran successfully");
        } catch (Exception e) {
            e.printStackTrace();
            throw new MojoExecutionException("Failed to run");
        }
    }
}
