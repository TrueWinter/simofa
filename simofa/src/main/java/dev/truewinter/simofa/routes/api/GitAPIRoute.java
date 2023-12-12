package dev.truewinter.simofa.routes.api;

import dev.truewinter.simofa.GitCredential;
import dev.truewinter.simofa.RouteLoader;
import dev.truewinter.simofa.routes.Route;
import io.javalin.http.Context;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
@RouteLoader.RouteClass()
public class GitAPIRoute extends Route {
    @RouteLoader.RouteInfo(
            url = "/api/git"
    )
    public void get(Context ctx) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("git", new ArrayList<>());

        try {
            List<GitCredential> git = getDatabase().getGitDatabase().getGitCredentials();
            resp.put("git", git);
        } catch (SQLException e) {
            e.printStackTrace();
            resp.put("success", false);
        }

        ctx.json(resp);
    }
}
