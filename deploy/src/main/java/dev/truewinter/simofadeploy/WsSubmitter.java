package dev.truewinter.simofadeploy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.truewinter.simofa.common.BuildStatus;
import dev.truewinter.simofa.common.SimofaLog;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletionStage;

public class WsSubmitter {
    private final ArrayList<String> queuedMessages = new ArrayList<>();
    private final WebSocket ws;
    private boolean disconnecting = false;

    public WsSubmitter(WebsiteDeployment websiteDeployment) {
        ws = HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(
                URI.create(websiteDeployment.getBuildUrl()), new WebSocket.Listener() {
            @Override
            public void onOpen(WebSocket webSocket) {
                WebSocket.Listener.super.onOpen(webSocket);
                websiteDeployment.setSubmitter(WsSubmitter.this);
                queuedMessages.forEach(s -> ws.sendText(s, true));
                queuedMessages.clear();
            }

            @Override
            public void onError(WebSocket webSocket, Throwable error) {
                WebSocket.Listener.super.onError(webSocket, error);
                SimofaDeploy.getLogger().error("WebSocket error", error);
            }
        }).join();
    }

    public void submitLog(SimofaLog log) {
        submit("log", log);
    }

    public void submitStatus(BuildStatus status) {
        submit("status", status);
    }

    public void disconnect() {
        if (disconnecting) return;
        if (!ws.isOutputClosed()) {
            disconnecting = true;
            // Wait 10 seconds for final logs to be collected from BufferedReader
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    ws.sendClose(WebSocket.NORMAL_CLOSURE, "Closed");
                }
            }, 10 * 1000);
        }
    }

    private void submit(String type, Object data) {
        HashMap<String, Object> json = new HashMap<>();
        json.put("type", type);
        json.put("data", data);

        try {
            String jsonString = new ObjectMapper().writeValueAsString(json);

            if (ws.isOutputClosed()) {
                queuedMessages.add(jsonString);
            } else {
                ws.sendText(jsonString, true);
            }
        } catch (JsonProcessingException e) {
            SimofaDeploy.getLogger().error("Failed to submit data", e);
        }
    }
}
