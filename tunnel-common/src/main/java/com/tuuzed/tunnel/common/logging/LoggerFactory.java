package com.tuuzed.tunnel.common.logging;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LoggerFactory {

    private LoggerFactory() {
        throw new IllegalStateException();
    }

    static final Map<String, LogAdapter> logAdapters = new ConcurrentHashMap<>();

    static {
        logAdapters.put("logcat", new LogcatLogAdapter());
    }

    @NotNull
    public static Logger getLogger(@NotNull Class clazz) {
        return getCreator().getLogger(clazz);
    }

    @NotNull
    public static Logger getLogger(@NotNull String name) {
        return getCreator().getLogger(name);
    }

    public static void addLogAdapter(@NotNull String name, @NotNull LogAdapter logAdapter) {
        if (logAdapters.containsKey(name)) {
            throw new IllegalArgumentException("contains name: " + name);
        }
        logAdapters.put(name, logAdapter);
    }

    @Nullable
    public static LogAdapter getLogAdapter(@NotNull String name) {
        return logAdapters.get(name);
    }

    public static void clearLogAdapters() {
        logAdapters.clear();
    }

    public static void setCreator(@NotNull LoggerCreator creator) {
        LoggerFactory.creator = creator;
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

    @NotNull
    private static LoggerCreator getCreator() {
        return creator;
    }

    interface LoggerCreator {
        @NotNull
        Logger getLogger(@NotNull String name);

        @NotNull
        Logger getLogger(@NotNull Class clazz);
    }

}
