package dev.truewinter.simofadeploy;

import dev.truewinter.simofa.common.Util;
import net.william278.annotaml.Annotaml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

public class SimofaDeploy {
    private static final Logger logger = LoggerFactory.getLogger(SimofaDeploy.class);
    private static WebServer server;
    private static DeployQueueManager deployQueueManager;

    public static void main(String[] args) throws Exception {
        try {
            logger.info("Starting Simofa Deploy v" + Util.getVersion());
        } catch (IOException e) {
            logger.info("Starting Simofa Deploy (unknown version)");
        }

        logger.info("Loading config");
        Config config = Annotaml.create(
                Path.of(Util.getInstallPath(), "deploy-config.yml").toFile(),
                Config.class).get();
        logger.info("Loaded config");

        if (config.getKey().equals("simofa")) {
            logger.warn("Using default secret, please change this in the config file.");
        }

        logger.info("Starting deploy queue manager");
        deployQueueManager = DeployQueueManager.getInstance();
        logger.info("Deploy queue manager started");

        logger.info("Starting web server");
        server = new WebServer(config);
        server.start();
        logger.info("Web server started");

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                shutdown();
            }
        });
    }

    public static DeployQueueManager getDeployQueueManager() {
        return deployQueueManager;
    }

    private static void shutdown() {
        if (server != null) {
            logger.info("Stopping web server");
            server.stopServer();
        }

        if (deployQueueManager != null) {
            logger.info("Stopping deploy queue manager");
            deployQueueManager.stop();
        }
    }

    public static Logger getLogger() {
        return logger;
    }
}
