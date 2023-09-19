package dev.truewinter.simofadeploy;

import dev.truewinter.simofa.common.BuildStatus;
import org.jetbrains.annotations.NotNull;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class StatusSubmitter extends Submitter<BuildStatus> {
    private BuildStatus previousStatus = BuildStatus.DEPLOYING;

    public StatusSubmitter(BuildServer buildServer) {
        super(buildServer);
    }

    public void post(@NotNull String status, int attempt) throws Exception {
        if (attempt >= Submitter.RETRIES) {
            throw new Exception("Failed to post status");
        }

        try {
            URL url = new URL(buildUrl + "/status/set");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Key", key);
            connection.setDoOutput(true);
            connection.setConnectTimeout(HTTP_TIMEOUT);
            connection.setReadTimeout(HTTP_TIMEOUT);
            connection.connect();

            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(status.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                SimofaDeploy.getLogger().warn(String.format("Received non-200 response for status submission: %d", responseCode));
            }

            connection.disconnect();
        } catch (Exception e) {
            SimofaDeploy.getLogger().error("An error occurred while submitting status", e);
            post(status, attempt + 1);
        }
    }

    @Override
    public void submit(@NotNull BuildStatus status, @NotNull SubmitterCallback callback) {
        new Thread(() -> {
            if (status.equals(previousStatus) ||
                    previousStatus.equals(BuildStatus.ERROR)) {
                callback.done();
                return;
            }
            previousStatus = status;

            try {
                post(status.toString(), 0);
            } catch (Exception e) {
                callback.error(e);
                return;
            }

            callback.done();
        }).start();
    }
}
