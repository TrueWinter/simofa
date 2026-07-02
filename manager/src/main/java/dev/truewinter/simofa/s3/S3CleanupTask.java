package dev.truewinter.simofa.s3;

import dev.truewinter.simofa.Simofa;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class S3CleanupTask {
    private static final Long MAX_OBJECT_LIFETIME = Duration.ofMinutes(15).toMillis();
    private static final Map<String, Long> fileRegistry = new HashMap<>();
    private final S3Service s3;

    public S3CleanupTask(S3Service s3) {
        this.s3 = s3;
    }

    public void run() {
        for (Map.Entry<String, Long> entry : fileRegistry.entrySet()) {
            try {
                String s3Path = entry.getKey();
                long uploadedAt = entry.getValue();
                long timeElapsed = Instant.now().toEpochMilli() - uploadedAt;

                if (timeElapsed > MAX_OBJECT_LIFETIME) {
                    s3.delete(s3Path);
                }
            } catch (Exception e) {
                Simofa.getLogger().error("Failed to delete artifact from S3", e);
            }
        }
    }

    public static void addFile(String s3Path) {
        fileRegistry.put(s3Path, Instant.now().toEpochMilli());
    }
}
