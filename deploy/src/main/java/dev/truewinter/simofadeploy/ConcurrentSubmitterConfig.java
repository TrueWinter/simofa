package dev.truewinter.simofadeploy;

public class ConcurrentSubmitterConfig<S extends Submitter<T>, T> {
    private final S submitter;
    private final T data;

    public ConcurrentSubmitterConfig(S submitter, T data) {
        this.data = data;
        this.submitter = submitter;
    }

    public S getSubmitter() {
        return submitter;
    }

    public T getData() {
        return data;
    }
}
