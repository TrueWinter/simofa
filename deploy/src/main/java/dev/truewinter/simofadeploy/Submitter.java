package dev.truewinter.simofadeploy;

import org.jetbrains.annotations.NotNull;

public abstract class Submitter<T> {
    public static final int HTTP_TIMEOUT = 5000;
    public static final int RETRIES = 3;

    protected final String buildUrl;
    protected final String key;

    public Submitter(BuildServer buildServer) {
        this.buildUrl = buildServer.buildUrl();
        this.key = buildServer.key();
    }

    abstract void submit(@NotNull T t, @NotNull SubmitterCallback callback);

    public interface SubmitterCallback {
        void done();
        void error(Exception e);
    }

    public static class NoopSubmitterCallback implements SubmitterCallback {
        @Override
        public void done() {}

        @Override
        public void error(Exception e) {}
    }
}
