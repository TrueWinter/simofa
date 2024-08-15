package dev.truewinter.simofa.routes;

import dev.truewinter.simofa.RouteLoader;
import dev.truewinter.simofa.Simofa;
import dev.truewinter.simofa.api.Website;
import dev.truewinter.simofa.routes.Route;
import io.javalin.http.*;

import java.sql.SQLException;
import java.util.*;

@SuppressWarnings("unused")
@RouteLoader.RouteClass()
public class WebsitesRoute extends Route {
    @RouteLoader.RouteInfo(
            url = "/api/websites"
    )
    public void getAllWebsites(Context ctx) {
        try {
            ctx.json(getDatabase().getWebsiteDatabase().getWebsites());
        } catch (SQLException e) {
            Simofa.getLogger().error("Failed to get websites", e);
            throw new InternalServerErrorResponse("Failed to get websites");
        }
    }

    @RouteLoader.RouteInfo(
            url = "/api/websites/{id}"
    )
    public void getOneWebsite(Context ctx) {
        String id = ctx.pathParam("id");

        try {
            Optional<Website> website = getDatabase().getWebsiteDatabase().getWebsiteById(id);
            website.ifPresentOrElse(ctx::json, () -> {
                throw new NotFoundResponse("Website not found");
            });
        } catch (SQLException e) {
            Simofa.getLogger().error("Failed to get website", e);
            throw new InternalServerErrorResponse("Failed to get website");
        }
    }

    @RouteLoader.RouteInfo(
            url = "/api/websites",
            method = HandlerType.POST
    )
    public void addWebsite(Context ctx) throws Exception {
        Website website = toWebsite(ctx);

        try {
            String id = getDatabase().getWebsiteDatabase().addWebsite(website);

            HashMap<String, String> resp = new HashMap<>();
            resp.put("id", id);
            ctx.json(resp);
        } catch (SQLException e) {
            Simofa.getLogger().error("Failed to add website", e);
            throw new InternalServerErrorResponse("Failed to add website");
        }
    }

    @RouteLoader.RouteInfo(
            url = "/api/websites/{id}",
            method = HandlerType.PUT
    )
    public void editWebsite(Context ctx) throws Exception {
        String id = ctx.pathParam("id");
        Website website = toWebsite(ctx, id);

        try {
            getDatabase().getWebsiteDatabase().editWebsite(website);

            HashMap<String, Boolean> resp = new HashMap<>();
            resp.put("success", true);
            ctx.json(resp);
        } catch (SQLException e) {
            Simofa.getLogger().error("Failed to edit website", e);
            throw new InternalServerErrorResponse("Failed to edit website");
        }
    }

    @RouteLoader.RouteInfo(
            url = "/api/websites/{id}",
            method = HandlerType.DELETE
    )
    public void deleteWebsite(Context ctx) {
        String id = ctx.pathParam("id");

        try {
            getDatabase().getWebsiteDatabase().deleteWebsite(id);

            HashMap<String, Boolean> resp = new HashMap<>();
            resp.put("success", true);
            ctx.json(resp);
        } catch (SQLException e) {
            Simofa.getLogger().error("Failed to delete website", e);
            throw new InternalServerErrorResponse("Failed to delete website");
        }
    }

    private Website toWebsite(Context ctx) throws Exception {
        return toWebsite(ctx, null);
    }

    private Website toWebsite(Context ctx, String id) throws Exception {
        Website website = ctxToT(ctx, Website.class, id);
        String COMMAND_PREFIX = "#!/bin/bash";

        int memory = website.getMemory();
        if (memory < 64 || memory > 99999) {
            throw new BadRequestResponse("Invalid memory value. Must be between 64 and 99999");
        }

        double cpu = website.getCpu();
        if (cpu < 0.1 || cpu > 99) {
            throw new BadRequestResponse("Invalid CPU value. Must be between 0.1 and 99");
        }

        if (!website.getBuildCommand().startsWith(COMMAND_PREFIX) ||
                !website.getDeployCommand().startsWith(COMMAND_PREFIX) ||
                !website.getDeployFailedCommand().startsWith(COMMAND_PREFIX)) {
            throw new BadRequestResponse("Commands must start with " + COMMAND_PREFIX);
        }

        return website;
    }
}
