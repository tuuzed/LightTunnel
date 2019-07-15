package com.tuuzed.tunnel.common.logging;

import org.junit.Before;
import org.junit.Test;

public class PlatformLoggerTest {

    private Logger logger = LoggerFactory.getLogger(PlatformLoggerTest.class);

    @Before
    public void setUp() throws Exception {

    }


    @Test
    public void log() {
        logger.trace("trace->{}", "Logger");
        logger.debug("debug->{}", "Logger");
        logger.info("info->{}", "Logger");
        logger.warn("warn->{}", "Logger");
        logger.error("error->{}", "Logger");
    }
}