package com.tuuzed.tunnel.common.logging;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    public static String getLevenName(int level) {
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
            case PROMPT:
                return "PROMPT";
            default:
                return "      ";
        }
    }


}
