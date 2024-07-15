package dev.truewinter.simofa.routes;

import dev.truewinter.simofa.RouteLoader;
import dev.truewinter.simofa.Simofa;
import dev.truewinter.simofa.docker.Container;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;

import java.util.Optional;

@SuppressWarnings("unused")
@RouteLoader.RouteClass()
public class DeleteDockerContainerRoute extends Route {
    @RouteLoader.RouteInfo(
            url = "/builds/containers/{id}/delete",
            method = HandlerType.POST
    )
    public void post(Context ctx) {
        String id = ctx.pathParam("id");
        Optional<Container> container =  Simofa.getDockerManager().getContainers().stream().filter(c -> c.getId().equals(id)).findFirst();
        if (container.isEmpty()) {
            ctx.status(404).result("Container does not exist");
            return;
        }

        Simofa.getDockerManager().deleteContainer(id);
        Simofa.getBuildQueueManager().getBuildQueue().remove(container.get());
        redirect(ctx,"/builds/containers");
    }
}
