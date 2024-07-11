package dev.truewinter.simofa.api.internal;

import dev.truewinter.simofa.api.WebsiteBuild;
import org.jetbrains.annotations.ApiStatus;

import java.util.HashMap;
import java.util.function.BiConsumer;

@ApiStatus.Internal
public class SseRegistry {
    private static final HashMap<Instances, BiConsumer<WebsiteBuild, Object>> consumers = new HashMap<>();

    public static void registerSseConsumer(Instances instance, BiConsumer<WebsiteBuild, Object> consumer) {
        if (consumers.containsKey(instance)) {
            throw new IllegalStateException("SSE consumer for instance " + instance + " registered multiple times");
        }

        consumers.put(instance, consumer);
    }

    public static void accept(Instances instance, WebsiteBuild build, Object object) {
        BiConsumer<WebsiteBuild, Object> consumer = consumers.get(instance);

        if (consumer != null) {
            consumer.accept(build, object);
        }
    }

    public enum Instances {
        WEBSITE_LOGS;
    }
}
