package dev.truewinter.simofa.docker;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.WaitResponse;

public abstract class DockerCallback implements ResultCallback<Frame> {
    abstract void created(String containerId);
    abstract void onExit(WaitResponse waitResponse);
}
