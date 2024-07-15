package dev.truewinter.simofa;

import dev.truewinter.simofa.api.SimofaPluginManager;
import dev.truewinter.simofa.common.Util;
import dev.truewinter.simofa.config.Config;
import dev.truewinter.simofa.config.MigratorConfig;
import dev.truewinter.simofa.database.Database;
import dev.truewinter.simofa.docker.BuildQueueManager;
import dev.truewinter.simofa.docker.DockerManager;
import dev.truewinter.simofa.docker.Image;
import dev.truewinter.simofa.migrator.Migrator;
import net.william278.annotaml.Annotaml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class Simofa {
    private static Config config;
    private static WebServer webServer;
    private static Database database;
    private static Logger logger = LoggerFactory.getLogger(Simofa.class);
    private static String secret;
    private static DockerManager dockerManager;
    private static BuildQueueManager buildQueueManager;
    private static SimofaPluginManager pluginManager;

    public static void main(String[] args) throws Exception {
        try {
            logger.info("Starting Simofa v" + Util.getVersion());
        } catch (IOException e) {
            logger.info("Starting Simofa (unknown version)");
        }

        logger.info("Loading config");
        config = Annotaml.create(
                Path.of(Util.getInstallPath(), "config.yml").toFile(),
                Config.class).get();
        logger.info("Loaded config");

        if (config.isDevMode()) {
            logger.warn("Running in dev mode");
        }

        secret = config.getSecret();

        logger.info("Loading migration config");
        MigratorConfig.init(Path.of(Util.getInstallPath(), "migrator.yml").toFile());
        logger.info("Loaded migration config");

        logger.info("Initializing database");
        Simofa.database = new Database(config);
        logger.info("Initialized database");

        logger.info("Running migrations");
        Migrator migrator = new Migrator(database);
        migrator.applyMigrations();
        logger.info("Ran migrations");

        logger.info("Connecting to Docker");
        dockerManager = DockerManager.getInstance(config);
        logger.info("Connected to Docker");

        List<Image> dockerImages = dockerManager.getImages();
        database.getWebsiteDatabase().getWebsites().forEach(w -> {
            if (dockerImages.stream().noneMatch(i -> i.getName().equals(w.getDockerImage()))) {
                logger.warn(String.format("Website %s uses non-existent Docker image %s", w.getName(), w.getDockerImage()));
            }
        });

        logger.info("Starting web server");
        webServer = new WebServer(config, database);
        webServer.start();
        logger.info("Started web server");

        if (config.getSecret().equals("simofa")) {
            logger.warn("Using default secret, please change this in the config file.");
        }

        if (config.getUrl().equals("http://localhost:8808")) {
            logger.warn("Using default URL, please change this in the config file.");
        }

        if (config.getConcurrentBuilds() == 0) {
            logger.warn("Concurrent builds is set to 0. No builds will run.");
        }

        if (Util.isBlank(config.getCacheDir())) {
            logger.warn("Cache directory not set. Build caching will be skipped.");
        } else {
            File cacheDir = new File(config.getCacheDir());
            if (!cacheDir.exists() && !cacheDir.mkdir()) {
                logger.warn("Failed to create cache directory, disabling build caching.");
                config.disableCache();
            }
        }

        logger.info("Initializing build queue manager");
        buildQueueManager = BuildQueueManager.getInstance(config, database);
        buildQueueManager.start();
        logger.info("Initialized build queue manager");

        logger.info("Loading plugins");
        pluginManager = SimofaPluginManager.getInstance(new API(database, webServer));
        pluginManager.getPluginManager().loadPlugins(Util.getPluginJars());
        logger.info("Loaded plugins");

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                shutdown();
            }
        });
    }

    public static Logger getLogger() {
        return logger;
    }

    protected static String getSecret() {
        return secret;
    }

    public static DockerManager getDockerManager() {
        return dockerManager;
    }

    public static BuildQueueManager getBuildQueueManager() {
        return buildQueueManager;
    }

    public static void shutdown() {
        logger.info("Shutting down");

        if (pluginManager != null) {
            pluginManager.getPluginManager().handleShutdown();
        }

        if (webServer != null) {
            logger.info("Stopping web server");
            webServer.stopServer();
        }

        if (dockerManager != null) {
            try {
                logger.info("Disconnecting from Docker");
                dockerManager.shutdown();
            } catch (Exception e) {
                logger.error("Failed to disconnect from Docker", e);
            }
        }

        if (database != null) {
            logger.info("Disconnecting from database");
            database.close();
        }
    }
}
