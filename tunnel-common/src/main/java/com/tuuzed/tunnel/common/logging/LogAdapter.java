package com.tuuzed.tunnel.common.logging;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface LogAdapter {
    boolean isLoggable(int level);

    void log(int level, @NotNull String msg, @Nullable Throwable cause);
}
