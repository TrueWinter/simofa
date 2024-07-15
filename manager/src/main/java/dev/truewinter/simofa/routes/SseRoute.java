package dev.truewinter.simofa.routes;

import dev.truewinter.simofa.routes.api.BuildLogsSseRoute;

import io.javalin.http.Context;
import io.javalin.http.sse.SseClient;

import java.util.HashMap;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

public class SseRoute extends Route {
    public static void handle(SseClient client, Consumer<SseClient> handler) {
        Context ctx = client.ctx();

        if (!Route.isLoggedIn(ctx)) {
            client.sendEvent("error", "Invalid cookie");
            return;
        }

        handler.accept(client);
    }

    public static void init() {
        BuildLogsSseRoute.init();

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                BuildLogsSseRoute.ping();
            }
        }, 0, 30 * 1000);
    }

    public static void ping(HashMap<String, Queue<SseClient>> clients) {
        clients.forEach((s, q) -> {
            q.forEach(c -> {
                c.sendComment("PING");
            });
        });
    }
}
