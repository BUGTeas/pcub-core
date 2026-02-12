package org.pcub.core.bukkit;

import org.bukkit.plugin.java.JavaPlugin;
import org.pcub.core.common.PCUBCoreLogger;

import java.util.logging.Level;
import java.util.logging.Logger;

public class BukkitLogger implements PCUBCoreLogger {
    private final Logger logger;

    public BukkitLogger(JavaPlugin plugin) {
        this.logger = plugin.getLogger();
    }

    @Override
    public void error(String message) {
        logger.severe(message);
    }

    @Override
    public void error(String message, Throwable error) {
        logger.log(Level.SEVERE, message, error);
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
        if (isDebug()) {
            logger.info("DEBUG " + message);
        }
    }

    @Override
    public boolean isDebug() {
        return true;//todo: 配置文件系统
    }
}
