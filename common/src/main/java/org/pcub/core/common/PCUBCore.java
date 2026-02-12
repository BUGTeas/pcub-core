package org.pcub.core.common;

public class PCUBCore {

    private static PCUBCore instance;
    private static PCUBCoreLogger logger;

    public static PCUBCoreLogger logger() {
        return logger;
    }

    public static void setCommon(PCUBCoreLogger logger) {
        PCUBCore.logger = logger;
    }

    public PCUBCore() {
    }


    public static PCUBCore load() {
        if (instance != null) {
            // reload
        }
        instance = new PCUBCore();
        return instance;
    }
}
