package dev.truewinter.simofa.api;

import dev.truewinter.PluginManager.Plugin;
import io.javalin.http.Handler;
import io.javalin.http.HandlerType;

import java.sql.SQLException;
import java.util.List;

@SuppressWarnings("unused")
public interface SimofaAPI {
    /**
     * Stops a running/queued build
     * @param websiteBuild The build
     */
    void stopBuild(WebsiteBuild websiteBuild);

    /**
     * Gets a {@link List} of all websites
     * @return A list of websites
     */
    List<Website> getWebsites() throws SQLException;

    /**
     * Adds a website to the database. The website ID will be ignored.
     */
    void addWebsite(Website website) throws SQLException;

    /**
     * Edits a website with the given ID, editing all database fields
     * to match the passed Website.
     */
    void editWebsite(Website website) throws SQLException;

    /**
     * Triggers a build, using the cache if available
     * @param website The website to build
     */
    void triggerBuild(Website website);

    /**
     * @see SimofaAPI#triggerBuild(Website)
     */
    void triggerBuild(Website website, boolean useCache);

    /**
     * Triggers a build using a custom commit message, using the cache if available
     * @param website The website to build
     */
    void triggerBuild(Website website, String message);

    /**
     * @see SimofaAPI#triggerBuild(Website, String)
     */
    void triggerBuild(Website website, String message, boolean useCache);

    /**
     * Returns a list of deployment servers
     */
    List<DeploymentServer> getDeploymentServers() throws SQLException;

    /**
     * Adds a deployment server. The ID will be ignored.
     */
    void addDeploymentServer(DeploymentServer deploymentServer) throws SQLException;

    /**
     * Edits a deployment server with the given ID, editing all database fields
     * to match the passed DeploymentServer.
     */
    void editDeploymentServer(DeploymentServer deploymentServer) throws SQLException;

    /**
     * Registers a route at /c/{plugin_name}/{path}
     */
    void registerRoute(Plugin<SimofaAPI> plugin, HandlerType method, String path, Handler handler);
}
