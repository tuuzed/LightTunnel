package com.tuuzed.tunnel.common.logging;

import org.jetbrains.annotations.NotNull;

public final class LogConfigurator {

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
    static LoggerCreator getCreator() {
        return creator;
    }

    public static void setCreator(@NotNull LoggerCreator creator) {
        LogConfigurator.creator = creator;
    }

    public static void setLevel(int level) {
        LogConfigurator.level = level;
    }

    public static boolean isPrintLog(int level) {
        return level >= LogConfigurator.level;
    }

    interface LoggerCreator {
        @NotNull
        Logger getLogger(@NotNull String name);

        @NotNull
        Logger getLogger(@NotNull Class clazz);
    }

}
