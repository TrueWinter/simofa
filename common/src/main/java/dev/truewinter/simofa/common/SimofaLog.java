package dev.truewinter.simofa.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.util.UUID;

public class SimofaLog {
    private final String type;
    private final String log;
    private final String uuid;
    private final long timestamp;

    public SimofaLog(LogType type, String log) {
        this.type = type.toString();
        this.log = log;
        // Ensures that there is something unique to use as a React key
        this.uuid = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
    }

    @SuppressWarnings("unused")
    public SimofaLog(@JsonProperty("type") String type,
                     @JsonProperty("log") String log,
                     @JsonProperty("uuid") String uuid,
                     @JsonProperty("timestamp") @JsonDeserialize(as = Long.class) long timestamp) {
        this.type = type;
        this.log = log;
        this.uuid = uuid;
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public String getLog() {
        return log;
    }

    @SuppressWarnings("unused")
    public String getUuid() {
        return uuid;
    }

    @SuppressWarnings("unused")
    @JsonSerialize(using = ToStringSerializer.class)
    public long getTimestamp() {
        return timestamp;
    }
}
