package com.tuuzed.tunnel.common.logging;

public final class LoggerFactory {

    public interface Creator {
        Logger getLogger(String name);

        Logger getLogger(Class clazz);
    }

    private static Creator creator = new PlatformLogger.Creator();

    public static void setCreator(Creator creator) {
        if (creator == null) {
            return;
        }
        LoggerFactory.creator = creator;
    }

    public static Logger getLogger(String name) {
        return creator.getLogger(name);
    }

    public static Logger getLogger(Class clazz) {
        return creator.getLogger(clazz);
    }
}
