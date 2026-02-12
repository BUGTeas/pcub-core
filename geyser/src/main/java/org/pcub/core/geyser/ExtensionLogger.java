package org.pcub.core.geyser;

import org.geysermc.geyser.api.extension.Extension;
import org.pcub.core.common.PCUBCoreLogger;

public class ExtensionLogger implements PCUBCoreLogger {
    private org.geysermc.geyser.api.extension.ExtensionLogger logger;

    public ExtensionLogger(Extension extension) {
        this.logger = extension.logger();
    }

    @Override
    public void error(String message) {
        logger.error(message);
    }

    @Override
    public void error(String message, Throwable error) {
        logger.error(message, error);
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
        logger.debug(message);
    }

    @Override
    public boolean isDebug() {
        return logger.isDebug();
    }
}
