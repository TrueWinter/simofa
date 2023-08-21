package dev.truewinter.simofa.routes;

import dev.truewinter.simofa.Simofa;
import dev.truewinter.simofa.docker.Container;
import io.javalin.http.Context;

import java.util.Optional;

public class DeleteDockerContainerRoute extends Route {
    @Override
    public void post(Context ctx) {
        String id = ctx.pathParam("id");
        Optional<Container> container =  Simofa.getDockerManager().getContainers().stream().filter(c -> c.getId().equals(id)).findFirst();
        if (container.isEmpty()) {
            ctx.status(404).result("Container does not exist");
            return;
        }

        Simofa.getDockerManager().deleteContainer(id);
        Simofa.getBuildQueueManager().getBuildQueue().remove(container.get());
        redirect(ctx,"/docker/containers");
    }
}
