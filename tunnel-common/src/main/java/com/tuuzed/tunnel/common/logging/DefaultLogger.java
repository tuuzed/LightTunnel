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
            .append(getLevenName(level)).append("] ")
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
        colorfulPrintln(log.toString(), 35, -1, 0);
        if (cause != null) {
            cause.printStackTrace(System.err);
        }
    }

    // 样式    : 0-空样式  1-粗体  4-下划线  7-反色
    // 颜色1   : 30-白色  31-红色 32-绿色 33-黄色  34  蓝色 35  紫色 36  浅蓝 37  灰色
    // 颜色2   : 90-白色  91-红色 92-绿色 93-黄色  94  蓝色 95  紫色 96  浅蓝 97  灰色
    // 背景 : 40-白色  41-红色 42-绿色 43-黄色  44  蓝色 45  紫色 46  浅蓝 47  灰色
    //
    // 格式: "\033[颜色;背景;样式m%s\033[0m"
    private void colorfulPrintln(String x, int color, int bg, int style) {
        if (bg <= 47 && bg >= 40) {
            System.err.printf("\033[%d;%d;%dm%s\033[%dm%n", color, bg, style, x, style);
        } else {
            System.err.printf("\033[%d;%dm%s\033[%dm%n", color, style, x, style);
        }
    }

}
