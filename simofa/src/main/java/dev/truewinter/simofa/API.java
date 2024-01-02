package dev.truewinter.simofa;

import dev.truewinter.simofa.api.DeploymentServer;
import dev.truewinter.simofa.api.SimofaAPI;
import dev.truewinter.simofa.api.Website;
import dev.truewinter.simofa.api.WebsiteBuild;
import dev.truewinter.simofa.database.Database;

import java.sql.SQLException;
import java.util.List;

public class API implements SimofaAPI {
    private final Database database;

    protected API(Database database) {
        this.database = database;
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
    public void addWebsite(Website website) throws SQLException {
        database.getWebsiteDatabase().addWebsite(website);
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
    public List<DeploymentServer> getDeploymentServers() throws SQLException {
        return database.getDeploymentServerDatabase().getDeploymentServers();
    }

    @Override
    public void addDeploymentServer(DeploymentServer deploymentServer) throws SQLException {
        database.getDeploymentServerDatabase().addDeploymentServer(deploymentServer);
    }

    @Override
    public void editDeploymentServer(DeploymentServer deploymentServer) throws SQLException {
        database.getDeploymentServerDatabase().editDeploymentServer(deploymentServer);
    }
}
