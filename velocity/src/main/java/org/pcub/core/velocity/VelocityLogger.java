package org.pcub.core.velocity;

import org.pcub.core.common.PCUBCoreLogger;
import java.util.logging.Logger;

//todo
public class VelocityLogger implements PCUBCoreLogger {
    private final Logger logger;

    public VelocityLogger() {
        this.logger = null;
    }

    @Override
    public void error(String message) {
        logger.severe(message);//todo test
    }

    @Override
    public void error(String message, Throwable error) {
        logger.severe(message + "\n" + error.getLocalizedMessage());//todo
    }

    @Override
    public void warning(String message) {
        logger.warning(message);
    }

    @Override
    public void info(String message) {
        logger.info(message);
    }

    @Override
    public void debug(String message) {
        logger.info("D " + message);//todo
    }

    @Override
    public boolean isDebug() {
        return true;//todo
    }
}
