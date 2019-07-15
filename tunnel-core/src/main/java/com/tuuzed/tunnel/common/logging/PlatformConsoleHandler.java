package com.tuuzed.tunnel.common.logging;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

class PlatformConsoleHandler extends StreamHandler {

    PlatformConsoleHandler() {
        setLevel(Level.ALL);
        setFormatter(new PlatformSimpleFormatter());
        try {
            setEncoding(null);
        } catch (Exception ex) {
            // pass
        }
        setOutputStream(System.err);
    }

    @Override
    public void publish(LogRecord record) {
        super.publish(record);
        flush();
    }

    @Override
    public void close() {
        flush();
    }

}
