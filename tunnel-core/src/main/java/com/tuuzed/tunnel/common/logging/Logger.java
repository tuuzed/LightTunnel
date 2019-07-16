package com.tuuzed.tunnel.common.logging;

import org.jetbrains.annotations.NotNull;

public interface Logger {

    void trace(@NotNull String format, Object... args);

    void trace(@NotNull String msg, Throwable cause);

    void debug(@NotNull String format, Object... args);

    void debug(@NotNull String msg, Throwable cause);

    void info(@NotNull String format, Object... args);

    void info(@NotNull String msg, Throwable cause);

    void warn(@NotNull String format, Object... args);

    void warn(@NotNull String msg, Throwable cause);

    void error(@NotNull String format, Object... args);

    void error(@NotNull String msg, Throwable cause);

}
