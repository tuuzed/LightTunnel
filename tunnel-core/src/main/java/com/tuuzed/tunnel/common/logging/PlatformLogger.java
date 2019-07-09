package com.tuuzed.tunnel.common.logging;

import org.jetbrains.annotations.Nullable;

public class PlatformLogger implements Logger {

    public static void setup() {
        LoggerFactory.setCreator(new Creator());
    }

    public static class Creator implements LoggerFactory.Creator {
        @Override
        public Logger getLogger(String name) {
            return new PlatformLogger(name);
        }

        @Override
        public Logger getLogger(Class clazz) {
            return new PlatformLogger(clazz);
        }
    }

    private final java.util.logging.Logger logger;

    public PlatformLogger(String name) {
        this.logger = java.util.logging.Logger.getLogger(name);
    }

    public PlatformLogger(Class clazz) {
        this.logger = java.util.logging.Logger.getLogger(clazz.getCanonicalName());
    }

    @Override
    public void trace(String format, Object... args) {
        log(java.util.logging.Level.FINE, formatMsg(format, args));
    }

    @Override
    public void trace(String msg, Throwable cause) {
        log(java.util.logging.Level.FINE, msg, cause);
    }

    @Override
    public void debug(String format, Object... args) {
        log(java.util.logging.Level.CONFIG, formatMsg(format, args));
    }

    @Override
    public void debug(String msg, Throwable cause) {
        log(java.util.logging.Level.CONFIG, msg, cause);
    }

    @Override
    public void info(String format, Object... args) {
        log(java.util.logging.Level.INFO, formatMsg(format, args));
    }

    @Override
    public void info(String msg, Throwable cause) {
        log(java.util.logging.Level.INFO, msg, cause);
    }

    @Override
    public void warn(String format, Object... args) {
        log(java.util.logging.Level.WARNING, formatMsg(format, args));
    }

    @Override
    public void warn(String msg, Throwable cause) {
        log(java.util.logging.Level.WARNING, msg, cause);
    }

    @Override
    public void error(String format, Object... args) {
        log(java.util.logging.Level.SEVERE, formatMsg(format, args));
    }

    @Override
    public void error(String msg, Throwable cause) {
        log(java.util.logging.Level.SEVERE, msg, cause);
    }

    private void log(java.util.logging.Level level, String format, Object... args) {
        String[] sourceClassAndMethod = getSourceClassAndMethod();
        if (sourceClassAndMethod != null) {
            logger.logp(level, sourceClassAndMethod[0], sourceClassAndMethod[1], formatMsg(format, args));
        } else {
            logger.log(level, formatMsg(format, args));
        }
    }

    private void log(java.util.logging.Level level, String msg, Throwable cause) {
        String[] sourceClassAndMethod = getSourceClassAndMethod();
        if (sourceClassAndMethod != null) {
            logger.logp(level, sourceClassAndMethod[0], sourceClassAndMethod[1], msg, cause);
        } else {
            logger.log(level, msg, cause);
        }
    }

    @Nullable
    private String[] getSourceClassAndMethod() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if (stackTrace.length >= 5) {
            return new String[]{stackTrace[4].getClassName(), stackTrace[4].getMethodName()};
        }
        return null;
    }

    private String formatMsg(String format, Object... args) {
        return LogFormatter.format(format, args);
    }
}
