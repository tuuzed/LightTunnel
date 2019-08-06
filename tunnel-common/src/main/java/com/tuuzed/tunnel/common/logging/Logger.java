package com.tuuzed.tunnel.common.logging;

import org.jetbrains.annotations.NotNull;

public interface Logger {

    int ALL = Integer.MIN_VALUE;
    int TRACE = 100;
    int DEBUG = 200;
    int INFO = 300;
    int WARN = 400;
    int ERROR = 500;
    int PROMPT = 600;
    int OFF = Integer.MAX_VALUE;


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

    void prompt(@NotNull String format, Object... args);

    void prompt(@NotNull String msg, Throwable cause);


}
