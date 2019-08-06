package com.tuuzed.tunnel.common.logging;

import org.jetbrains.annotations.NotNull;

public interface LogAdapter {
    boolean isLoggable(int level);

    void log(int level, @NotNull String msg, Throwable cause);
}
