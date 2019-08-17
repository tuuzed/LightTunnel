package com.tuuzed.tunnel.cli.common;

import org.apache.log4j.Level;

public enum LogLevel {

    ALL(Level.ALL),
    TRACE(Level.TRACE),
    DEBUG(Level.DEBUG),
    INFO(Level.INFO),
    WARN(Level.WARN),
    ERROR(Level.ERROR),
    OFF(Level.OFF);

    public final Level level;


    LogLevel(Level level) {
        this.level = level;
    }
}
