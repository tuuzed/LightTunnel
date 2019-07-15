package com.tuuzed.tunnel.common.logging;

import org.jetbrains.annotations.NotNull;

public final class LogFormatter {

    public static String format(@NotNull String format, Object... arguments) {
        String msg = format;
        for (Object arg : arguments) {
            int index = msg.indexOf("{}");
            if (index >= 0) {
                StringBuilder sb = new StringBuilder();
                sb.append(msg, 0, index);
                sb.append((arg == null) ? "null" : arg.toString());
                sb.append(msg, index + 2, msg.length());
                msg = sb.toString();
            }
        }
        return msg;
    }

}
