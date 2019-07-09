package com.tuuzed.tunnel.common.logging;

public interface Logger {
    void trace(String format, Object... args);

    void trace(String msg, Throwable cause);

    void debug(String format, Object... args);

    void debug(String msg, Throwable cause);

    void info(String format, Object... args);

    void info(String msg, Throwable cause);

    void warn(String format, Object... args);

    void warn(String msg, Throwable cause);

    void error(String format, Object... args);

    void error(String msg, Throwable cause);

}
