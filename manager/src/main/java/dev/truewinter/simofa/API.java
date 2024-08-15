package dev.truewinter.simofa;

import dev.truewinter.PluginManager.Plugin;
import dev.truewinter.simofa.api.DeployServer;
import dev.truewinter.simofa.api.SimofaAPI;
import dev.truewinter.simofa.api.Website;
import dev.truewinter.simofa.api.WebsiteBuild;
import dev.truewinter.simofa.database.Database;
import io.javalin.http.Handler;
import io.javalin.http.HandlerType;

import java.sql.SQLException;
import java.util.List;

public class API implements SimofaAPI {
    private final Database database;
    private final WebServer server;

    protected API(Database database, WebServer server) {
        this.database = database;
        this.server = server;
    }

    @Override
    public void stopBuild(WebsiteBuild websiteBuild) {
        Simofa.getBuildQueueManager().getBuildQueue().remove(websiteBuild.getWebsite());
    }

    @Override
    public List<Website> getWebsites() throws SQLException {
        return database.getWebsiteDatabase().getWebsites();
    }

    @Override
    public String addWebsite(Website website) throws SQLException {
        return database.getWebsiteDatabase().addWebsite(website);
    }

    @Override
    public void editWebsite(Website website) throws SQLException {
        database.getWebsiteDatabase().editWebsite(website);
    }

    @Override
    public void triggerBuild(Website website) {
        triggerBuild(website, "<manual build>");
    }

    private String getCommitMessage(String message, boolean useCache) {
        if (!useCache) {
            message = "[no cache] " + message;
        }

        return message;
    }

    @Override
    public void triggerBuild(Website website, boolean useCache) {
        triggerBuild(website, getCommitMessage("<manual build>" , useCache));
    }

    @Override
    public void triggerBuild(Website website, String message) {
        Simofa.getBuildQueueManager().getBuildQueue().queue(website, message);
    }

    @Override
    public void triggerBuild(Website website, String message, boolean useCache) {
        triggerBuild(website, getCommitMessage(message, useCache));
    }

    @Override
    public List<DeployServer> getDeploymentServers() throws SQLException {
        return database.getDeployServerDatabase().getDeployServers();
    }

    @Override
    public void addDeploymentServer(DeployServer deployServer) throws SQLException {
        database.getDeployServerDatabase().addDeployServer(deployServer);
    }

    @Override
    public void editDeploymentServer(DeployServer deployServer) throws SQLException {
        database.getDeployServerDatabase().editDeployServer(deployServer);
    }

    @Override
    public void registerRoute(Plugin<SimofaAPI> plugin, HandlerType method, String path, Handler handler) {
        server.registerRoute(method, String.format("/c/%s/%s", plugin.getName(), path), handler);
    }
}
