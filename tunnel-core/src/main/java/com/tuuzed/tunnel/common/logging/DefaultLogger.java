package com.tuuzed.tunnel.common.logging;

import org.jetbrains.annotations.NotNull;

import java.util.Date;

public class DefaultLogger implements Logger {

    private static final int TRACE = 100;
    private static final int DEBUG = 200;
    private static final int INFO = 300;
    private static final int WARN = 400;
    private static final int ERROR = 500;

    @NotNull
    private String name;

    private final Date date = new Date();

    public DefaultLogger(@NotNull String name) {
        this.name = name;
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

    private synchronized void log(int level, @NotNull String format, Object... args) {
        if (!isPrintLog(level)) {
            return;
        }
        date.setTime(System.currentTimeMillis());
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if (stackTrace.length >= 3) {
            System.err.printf("%1$tF %1$tT %5$s %2$s#%3$s [%4$s]: %6$s%n",
                    date,
                    name,
                    stackTrace[3].getMethodName(),
                    getLevenName(level),
                    Thread.currentThread().getName(),
                    LogFormatter.format(format, args)
            );
        } else {
            System.err.printf("%1$tF %1$tT %4$s %2$s [%3$s]: %5$s%n",
                    date,
                    name,
                    getLevenName(level),
                    Thread.currentThread().getName(),
                    LogFormatter.format(format, args)
            );
        }
    }

    private synchronized void log(int level, @NotNull String msg, Throwable cause) {
        if (!isPrintLog(level)) {
            return;
        }
        date.setTime(System.currentTimeMillis());
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if (stackTrace.length >= 3) {
            System.err.printf("%1$tF %1$tT %5$s %2$s#%3$s [%4$s]: %6$s%n",
                    date,
                    name,
                    stackTrace[3].getMethodName(),
                    getLevenName(level),
                    Thread.currentThread().getName(),
                    msg
            );
        } else {
            System.err.printf("%1$tF %1$tT %4$s %2$s [%3$s]: %5$s%n",
                    date,
                    name,
                    getLevenName(level),
                    Thread.currentThread().getName(),
                    msg
            );
        }
        if (cause != null) {
            cause.printStackTrace(System.err);
        }
    }

    private boolean isPrintLog(int level) {
        if (level >= DEBUG) {
            return true;
        }
        return false;
    }

    @NotNull
    private String getLevenName(int level) {
        switch (level) {
            case TRACE:
                return "TRACE";
            case DEBUG:
                return "DEBUG";
            case INFO:
                return "INFO";
            case WARN:
                return "WARN";
            case ERROR:
                return "ERROR";
            default:
                return "";
        }
    }
}
