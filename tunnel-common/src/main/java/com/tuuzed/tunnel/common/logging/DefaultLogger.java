package com.tuuzed.tunnel.common.logging;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

import static com.tuuzed.tunnel.common.logging.LoggerFactory.isLoggable;
import static com.tuuzed.tunnel.common.logging.Utils.*;


public class DefaultLogger implements Logger {
    @NotNull
    private String name;
    private String shortName;
    private final Date date = new Date();

    public DefaultLogger(@NotNull String name) {
        this.name = name;
        this.shortName = getShortClassName(name);
    }

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
        log(PROMPT, msg, cause);
    }

    private void log(int level, @NotNull String format, Object... args) {
        if (!isLoggable(level)) {
            return;
        }
        StackTraceElement[] traces = Thread.currentThread().getStackTrace();
        if (traces.length >= 3) {
            log(level, formatPlaceholderMsg(format, args), null, traces[3]);
        } else {
            log(level, formatPlaceholderMsg(format, args), null, null);
        }
    }

    private void log(int level, @NotNull String msg, @Nullable Throwable cause) {
        if (!isLoggable(level)) {
            return;
        }
        StackTraceElement[] traces = Thread.currentThread().getStackTrace();
        if (traces.length >= 3) {
            log(level, msg, cause, traces[3]);
        } else {
            log(level, msg, cause, null);
        }
    }

    private synchronized void log(
        int level,
        @NotNull String msg,
        @Nullable Throwable cause,
        @Nullable StackTraceElement trace
    ) {
        date.setTime(System.currentTimeMillis());
        StringBuilder log = new StringBuilder();
        log
            .append(formatDate(date)).append(" [")
            .append(getLevelName(level)).append("] ")
            .append(Thread.currentThread().getName()).append(" ");

        if (trace != null) {
            log
                .append(getShortClassName(trace.getClassName())).append("#")
                .append(trace.getMethodName()).append(": ");
        } else {
            log
                .append(shortName).append(": ");
        }
        log.append(msg);
        printlnColored(log.toString(), getLevelColor(level), -1, -1, System.err);
        if (cause != null) {
            cause.printStackTrace(System.err);
        }
    }


}
