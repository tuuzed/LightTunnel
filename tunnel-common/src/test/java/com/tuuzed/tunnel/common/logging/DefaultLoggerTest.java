package com.tuuzed.tunnel.common.logging;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class DefaultLoggerTest {

    private Logger logger = LoggerFactory.getLogger(DefaultLoggerTest.class);

    @Before
    public void setUp() {
        LoggerFactory.addLogAdapter("disk", new DiskLogAdapter());
    }

    @Test
    public void log() throws Exception {

        for (int i = 0; i < 20; i++) {
            logger.trace("trace->{}", "Logger");
            logger.debug("debug->{}", "Logger");
            logger.info("info->{}", "Logger");
            logger.warn("warn->{}", "Logger");
            logger.error("error->{}", "Logger");
            logger.prompt("prompt->{}", "Logger");

            logger.trace("trace->", new Throwable());
            logger.debug("debug->", new Throwable());
            logger.info("info->", new Throwable());
            logger.warn("warn->", new Throwable());
            logger.error("error->", new Throwable());
            logger.prompt("prompt->", new Throwable());
            TimeUnit.SECONDS.sleep(1);
        }
        TimeUnit.SECONDS.sleep(2);
    }

}