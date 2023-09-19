package dev.truewinter.simofadeploy;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.truewinter.simofa.common.SimofaLog;
import org.jetbrains.annotations.NotNull;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class LogSubmitter extends Submitter<List<SimofaLog>> {
    public LogSubmitter(BuildServer buildServer) {
        super(buildServer);
    }

    private void post(@NotNull String json, int attempt) throws Exception {
        if (attempt >= Submitter.RETRIES) {
            throw new Exception("Failed to submit logs");
        }

        try {
            URL url = new URL(buildUrl + "/logs/add");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Key", key);
            connection.setDoOutput(true);
            connection.setConnectTimeout(HTTP_TIMEOUT);
            connection.setReadTimeout(HTTP_TIMEOUT);
            connection.connect();

            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(json.getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                SimofaDeploy.getLogger().warn(String.format("Received non-200 response for log submission: %d", responseCode));
            }

            connection.disconnect();
        } catch (Exception e) {
            SimofaDeploy.getLogger().error("An error occurred while submitting logs", e);
            post(json, attempt + 1);
        }
    }

    public void submit(@NotNull List<SimofaLog> logs, @NotNull SubmitterCallback callback) {
        new Thread(() -> {
            if (logs.size() == 0) {
                callback.done();
                return;
            }

            String logsJson = "";
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                logsJson = objectMapper.writeValueAsString(logs);

                post(logsJson, 0);
            } catch (Exception e) {
                callback.error(e);
                return;
            }

            callback.done();
        }).start();
    }
}
