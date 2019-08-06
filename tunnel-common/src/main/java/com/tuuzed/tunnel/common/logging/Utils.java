package com.tuuzed.tunnel.common.logging;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.tuuzed.tunnel.common.logging.Logger.*;

final class Utils {

    private final static String datePattern = "yyyy-MM-dd HH:mm:ss.SSS";
    private final static ThreadLocal<WeakReference<DateFormat>> sDateFormat = new ThreadLocal<>();

    @NotNull
    public static String formatDate(@Nullable Date date) {
        if (date == null) {
            return "";
        }
        WeakReference<DateFormat> weakReference = sDateFormat.get();
        DateFormat dateFormat;
        if (weakReference == null) {
            dateFormat = new SimpleDateFormat(datePattern);
            sDateFormat.set(new WeakReference<>(dateFormat));
        } else {
            dateFormat = weakReference.get();
            if (dateFormat == null) {
                dateFormat = new SimpleDateFormat(datePattern);
                sDateFormat.set(new WeakReference<>(dateFormat));
            }
        }
        return dateFormat.format(date);
    }

    @NotNull
    public static String formatPlaceholderMsg(@NotNull String format, @NotNull Object... arguments) {
        String msg = format;
        for (Object arg : arguments) {
            int index = msg.indexOf("{}");
            if (index > -1) {
                StringBuilder sb = new StringBuilder();
                sb.append(msg, 0, index);
                sb.append((arg == null) ? "null" : arg.toString());
                sb.append(msg, index + 2, msg.length());
                msg = sb.toString();
            }
        }
        return msg;
    }

    @NotNull
    public static String getShortClassName(@NotNull String className) {
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
    public static String getLevelName(int level) {
        switch (level) {
            case TRACE:
                return "TRACE ";
            case DEBUG:
                return "DEBUG ";
            case INFO:
                return "INFO  ";
            case WARN:
                return "WARN  ";
            case ERROR:
                return "ERROR ";
            case FATAL:
                return "FATAL ";
            case PROMPT:
                return "PROMPT";
            default:
                return "      ";
        }
    }

    public static int getLevelColor(int level) {
        switch (level) {
            case TRACE:
                return 37;
            case DEBUG:
                return 37;
            case INFO:
                return 36;
            case WARN:
                return 93;
            case ERROR:
                return 91;
            case FATAL:
                return 91;
            case PROMPT:
                return 90;
            default:
                return -1;
        }
    }

    // 样式    : 0-空样式  1-粗体  4-下划线  7-反色
    // 颜色1   : 30-白色  31-红色  32-绿色  33-黄色  34-蓝色  35-紫红色  36-青蓝色  37-灰色
    // 颜色2   : 90-白色  91-红色  92-绿色  93-黄色  94-蓝色  95-紫红色  96-青蓝色  97-灰色
    // 背景    : 40-白色  41-红色  42-绿色  43-黄色  44-蓝色  45-紫红色  46-青蓝色  47-灰色
    // 格式: "\033[颜色;背景;样式m%s\033[0m"
    public static void printlnColored(@NotNull String x, int color, int bg, int style, @NotNull PrintStream out) {
        String scolor = "";
        String sbg = "";
        String sstyle = "0";
        if ((color >= 30 && color <= 37) || (color >= 90 && color <= 97)) {
            scolor = String.valueOf(color);
        }
        if (bg >= 40 && bg <= 47) {
            sbg = String.valueOf(bg);
        }
        if (style == 0 || style == 1 || style == 4 || style == 7) {
            sstyle = String.valueOf(style);
        }
        out.printf("\033[%s;%s;%sm%s\033[%sm%n", scolor, sbg, sstyle, x, sstyle);
    }


}
