package dev.truewinter.simofa.routes.api;

import dev.truewinter.simofa.RouteLoader;
import dev.truewinter.simofa.Simofa;
import dev.truewinter.simofa.api.Website;
import dev.truewinter.simofa.common.Util;
import dev.truewinter.simofa.api.WebsiteBuild;
import dev.truewinter.simofa.routes.Route;
import io.javalin.http.Context;

import java.util.*;

@SuppressWarnings("unused")
@RouteLoader.RouteClass()
public class QueueAPI extends Route {
    // Shared between `/websites/{id}/logs` and `/builds/queue`
    @RouteLoader.RouteInfo(
            url = "/api/queue"
    )
    public void get(Context ctx) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("success", true);

        String websiteIdString = ctx.queryParam("website");

        if (!Util.isBlank(websiteIdString)) {
            try {
                int websiteId = Integer.parseInt(websiteIdString);
                Optional<Website> website = getDatabase().getWebsiteDatabase().getWebsiteById(websiteId);
                if (website.isPresent()) {
                    List<WebsiteBuild> websiteBuildList = new ArrayList<>(Simofa.getBuildQueueManager().getBuildQueue()
                            .getWebsiteBuildList().getOrDefault(websiteId, new ArrayList<>()));
                    sort(websiteBuildList);
                    map.put("queue", websiteBuildList);
                } else {
                    map.put("success", false);
                    map.put("error", "Website not found");
                }
            } catch(Exception e) {
                e.printStackTrace();
                map.put("success", false);
                map.put("error", "An error occurred");
            }
        } else {
            List<WebsiteBuild> websiteBuildList = new ArrayList<>();
            Simofa.getBuildQueueManager().getBuildQueue().getWebsiteBuildList()
                    .values().forEach(websiteBuildList::addAll);
            sort(websiteBuildList);
            map.put("queue", websiteBuildList);
        }

        ctx.json(map);
    }

    private void sort(List<WebsiteBuild> list) {
        list.sort((a, b) -> {
            if (a.getStartTime() == b.getStartTime()) {
                return 0;
            }

            if (a.getStartTime() - b.getStartTime() > 0 || a.getStartTime() == 0) {
                return -1;
            } else {
                return 1;
            }
        });
    }
}
