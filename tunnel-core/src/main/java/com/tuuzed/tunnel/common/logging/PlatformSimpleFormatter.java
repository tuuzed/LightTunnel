package com.tuuzed.tunnel.common.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class PlatformSimpleFormatter extends Formatter {

    private static final String format = "%1$tF %1$tT [%2$s] [%3$s] %4$s: %5$s%6$s%n";
    private final Date dat = new Date();

    @Override
    public synchronized String format(LogRecord record) {
        dat.setTime(record.getMillis());
        String source;
        if (record.getSourceClassName() != null) {
            source = record.getSourceClassName();
            if (record.getSourceMethodName() != null) {
                source += " " + record.getSourceMethodName();
            }
        } else {
            source = record.getLoggerName();
        }
        String message = formatMessage(record);
        String throwable = "";
        if (record.getThrown() != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.println();
            record.getThrown().printStackTrace(pw);
            pw.close();
            throwable = sw.toString();
        }
        return String.format(format,
                dat,
                getLevelName(record.getLevel()),
                Thread.currentThread().getName(),
                source,
                message,
                throwable);
    }

    private static String getLevelName(Level level) {
        if (level == Level.FINE) {
            return "TRACE";
        } else if (level == Level.CONFIG) {
            return "DEBUG";
        } else if (level == Level.INFO) {
            return "INFO";
        } else if (level == Level.WARNING) {
            return "WARN";
        } else if (level == Level.SEVERE) {
            return "ERROR";
        } else {
            return level.getName();
        }

    }
}
