package org.pcub.core.common;

import java.util.function.Supplier;

public interface PCUBCoreLogger {
    /**
     * Logs an error message to console
     *
     * @param message the message to log
     */
    void error(String message);

    /**
     * Logs an error message and an exception to console
     *
     * @param message the message to log
     * @param error the error to throw
     */

    void error(String message, Throwable error);
    /**
     * Logs a warning message to console
     *
     * @param message the message to log
     */
    void warning(String message);

    /**
     * Logs an info message to console
     *
     * @param message the message to log
     */
    void info(String message);

    /**
     * Logs a debug message to console
     *
     * @param message the message to log
     */
    void debug(String message);

    /**
     * Logs a debug message to console
     *
     * @param message the message to log (Only parse if debug mode is enabled)
     */
    default void debug(Supplier<String> message) {
        if (isDebug()) {
            debug(message.get());
        }
    }

    /**
     * If debug is enabled for this logger
     */
    boolean isDebug();
}
