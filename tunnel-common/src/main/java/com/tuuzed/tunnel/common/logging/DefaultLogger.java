package com.tuuzed.tunnel.common.logging;

import org.jetbrains.annotations.NotNull;

import java.util.Date;

import static com.tuuzed.tunnel.common.logging.LogConfigurator.isPrintLog;

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

    private synchronized void log(int level, @NotNull String format, Object... args) {
        if (!isPrintLog(level)) {
            return;
        }
        date.setTime(System.currentTimeMillis());
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if (stackTrace.length >= 3) {
            System.err.printf("%1$tF %1$tT [%2$s] %3$s %4$s#%5$s=> %6$s%n",
                    date,
                    getLevenName(level),
                    Thread.currentThread().getName(),
                    getShortClassName(stackTrace[3].getClassName()),
                    stackTrace[3].getMethodName(),
                    LogFormatter.format(format, args)
            );
        } else {
            System.err.printf("%1$tF %1$tT [%2$s] %3$s %4$s=> %5$s%n",
                    date,
                    getLevenName(level),
                    Thread.currentThread().getName(),
                    shortName,
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
            System.err.printf("%1$tF %1$tT [%2$s] %3$s %4$s#%5$s=> %6$s%n",
                    date,
                    getLevenName(level),
                    Thread.currentThread().getName(),
                    getShortClassName(stackTrace[3].getClassName()),
                    stackTrace[3].getMethodName(),
                    msg
            );
        } else {
            System.err.printf("%1$tF %1$tT [%2$s] %3$s %4$s=> %5$s%n",
                    date,
                    getLevenName(level),
                    Thread.currentThread().getName(),
                    shortName,
                    msg
            );
        }
        if (cause != null) {
            cause.printStackTrace(System.err);
        }
    }

    private String getShortClassName(String className) {
        if (className.length() > 20) {
            String[] array = className.split("\\.");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < array.length - 1; i++) {
                sb.append(array[i], 0, 1);
                sb.append(".");
            }
            sb.append(array[array.length - 1]);
            return sb.toString();
        } else {
            return className;
        }
    }

    @NotNull
    private String getLevenName(int level) {
        switch (level) {
            case TRACE:
                return "TRACE";
            case DEBUG:
                return "DEBUG";
            case INFO:
                return "INFO ";
            case WARN:
                return "WARN ";
            case ERROR:
                return "ERROR";
            default:
                return "     ";
        }
    }
}
