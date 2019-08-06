package com.tuuzed.tunnel.common.logging;

import org.jetbrains.annotations.NotNull;

import static com.tuuzed.tunnel.common.logging.Utils.getLevelColor;
import static com.tuuzed.tunnel.common.logging.Utils.printlnColored;

public class LogcatLogAdapter implements LogAdapter {
    private int level = Logger.ALL;

    @Override
    public boolean isLoggable(int level) {
        return this.level < level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    @Override
    public void log(int level, @NotNull String msg, Throwable cause) {
        printlnColored(msg, getLevelColor(level), -1, -1, System.err);
        if (cause != null) {
            cause.printStackTrace(System.err);
        }
    }
}
