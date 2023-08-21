package dev.truewinter.simofa.docker;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.WaitResponse;
import org.jetbrains.annotations.ApiStatus;

import java.io.Closeable;
import java.io.IOException;

public abstract class DockerWaitCallback implements ResultCallback<WaitResponse> {
    @Override
    @ApiStatus.Internal
    public void onStart(Closeable closeable) {}

    @Override
    @ApiStatus.Internal
    public void onNext(WaitResponse waitResponse) {
        onExit(waitResponse);
    }

    public abstract void onExit(WaitResponse waitResponse);

    @Override
    @ApiStatus.Internal
    public void onError(Throwable throwable) {}

    @Override
    @ApiStatus.Internal
    public void onComplete() {}

    @Override
    @ApiStatus.Internal
    public void close() throws IOException {}
}
