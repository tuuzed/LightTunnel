package com.tuuzed.tunnel.common.logging;

import org.jetbrains.annotations.NotNull;

public final class LoggerFactory {

    @NotNull
    public static Logger getLogger(@NotNull Class clazz) {
        return getCreator().getLogger(clazz);
    }

    @NotNull
    public static Logger getLogger(@NotNull String name) {
        return getCreator().getLogger(name);
    }

    @NotNull
    private static LoggerCreator creator = new LoggerCreator() {
        @NotNull
        @Override
        public Logger getLogger(@NotNull String name) {
            return new DefaultLogger(name);
        }

        @NotNull
        @Override
        public Logger getLogger(@NotNull Class clazz) {
            return getLogger(clazz.getCanonicalName());
        }
    };

    private static int level = Logger.ALL;

    @NotNull
    private static LoggerCreator getCreator() {
        return creator;
    }

    public static void setCreator(@NotNull LoggerCreator creator) {
        LoggerFactory.creator = creator;
    }

    public static void setLevel(int level) {
        LoggerFactory.level = level;
    }

    public static boolean isLoggable(int level) {
        return LoggerFactory.level < level;
    }

    interface LoggerCreator {
        @NotNull
        Logger getLogger(@NotNull String name);

        @NotNull
        Logger getLogger(@NotNull Class clazz);
    }

}
