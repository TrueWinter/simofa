package dev.truewinter.simofa.api.internal;

import org.jetbrains.annotations.ApiStatus;

import java.util.HashMap;
import java.util.function.BiConsumer;

@ApiStatus.Internal
public class WsRegistry {
    private static final HashMap<Instances, BiConsumer<Object, Object>> consumers = new HashMap<>();

    public static void registerWsConsumer(Instances instance, BiConsumer<Object, Object> consumer) {
        if (consumers.containsKey(instance)) {
            throw new IllegalStateException("SSE consumer for instance " + instance + " registered multiple times");
        }

        consumers.put(instance, consumer);
    }

    public static void accept(Instances instance, Object audience, Object data) {
        // Data should not be sent to ingest routes. They exist solely to get data into Simofa.
        if (instance.toString().endsWith("_INGEST")) return;

        BiConsumer<Object, Object> consumer = consumers.get(instance);

        if (consumer != null) {
            consumer.accept(audience, data);
        }
    }

    public enum IngestInstances {
        DEPLOY_INGEST
    }

    public enum Instances {
        WEBSITE_LOGS,
        BUILD_QUEUE
    }
}
