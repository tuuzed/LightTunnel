package com.tuuzed.tunnel.common.logging;

import org.jetbrains.annotations.NotNull;

public abstract class AbstractLogger implements Logger {

    abstract void log(int level, @NotNull String format, Object... args);

    abstract void log(int level, @NotNull String msg, Throwable cause);

    @Override
    public void trace(@NotNull String format, Object... args) {
        log(TRACE, format, args);
    }

    @Override
    public void trace(@NotNull String msg, Throwable cause) {
        log(TRACE, msg, cause);
    }

    @Override
    public void debug(@NotNull String format, Object... args) {
        log(DEBUG, format, args);
    }

    @Override
    public void debug(@NotNull String msg, Throwable cause) {
        log(DEBUG, msg, cause);
    }

    @Override
    public void info(@NotNull String format, Object... args) {
        log(INFO, format, args);
    }

    @Override
    public void info(@NotNull String msg, Throwable cause) {
        log(INFO, msg, cause);
    }

    @Override
    public void warn(@NotNull String format, Object... args) {
        log(WARN, format, args);
    }

    @Override
    public void warn(@NotNull String msg, Throwable cause) {
        log(WARN, msg, cause);
    }

    @Override
    public void error(@NotNull String format, Object... args) {
        log(ERROR, format, args);
    }

    @Override
    public void error(@NotNull String msg, Throwable cause) {
        log(ERROR, msg, cause);
    }

    @Override
    public void fatal(@NotNull String format, Object... args) {
        log(FATAL, format, args);
    }

    @Override
    public void fatal(@NotNull String msg, Throwable cause) {
        log(FATAL, msg, cause);
    }

    @Override
    public void prompt(@NotNull String format, Object... args) {
        log(PROMPT, format, args);
    }

    @Override
    public void prompt(@NotNull String msg, Throwable cause) {
        log(TRACE, msg, cause);
    }
}
