package dev.truewinter.simofa.api;

import org.slf4j.LoggerFactory;

/**
 * @hidden
 */
public class L4jLogger {
    private static org.slf4j.Logger logger;

    protected static org.slf4j.Logger getL4jLogger() {
        if (logger == null) {
            logger = LoggerFactory.getLogger(SimofaPlugin.class);
        }

        return logger;
    }
}
