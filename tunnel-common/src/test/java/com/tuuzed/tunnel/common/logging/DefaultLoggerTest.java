package com.tuuzed.tunnel.common.logging;

import org.junit.Test;

public class DefaultLoggerTest {

    private Logger logger = LoggerFactory.getLogger(DefaultLoggerTest.class);

    @Test
    public void log() {
        LoggerFactory.setLevel(Logger.ALL);
        logger.trace("trace->{}", "Logger");
        logger.debug("debug->{}", "Logger");
        logger.info("info->{}", "Logger");
        logger.warn("warn->{}", "Logger");
        logger.error("error->{}", "Logger");
        logger.prompt("prompt->{}", "Logger");

        System.out.println("Hello,Akina!");
        System.out.println("\033[30;4m" + "Hello,Akina!" + "\033[0m");
        System.out.println("\033[31;4m" + "Hello,Akina!" + "\033[0m");
        System.out.println("\033[32;4m" + "Hello,Akina!" + "\033[0m");
        System.out.println("\033[33;4m" + "Hello,Akina!" + "\033[0m");
        System.out.println("\033[34;4m" + "Hello,Akina!" + "\033[0m");
        System.out.println("\033[35;4m" + "Hello,Akina!" + "\033[0m");
        System.out.println("\033[36;4m" + "Hello,Akina!" + "\033[0m");
        System.out.println("\033[37;4m" + "Hello,Akina!" + "\033[0m");
        System.out.println("\033[40;31;4m" + "Hello,Akina!" + "\033[0m");
        System.out.println("\033[41;32;4m" + "Hello,Akina!" + "\033[0m");
        System.out.println("\033[42;33;4m" + "Hello,Akina!" + "\033[0m");
        System.out.println("\033[43;34;4m" + "Hello,Akina!" + "\033[0m");
        System.out.println("\033[44;35;4m" + "Hello,Akina!" + "\033[0m");
        System.out.println("\033[45;36;4m" + "Hello,Akina!" + "\033[0m");
        System.out.println("\033[46;37;4m" + "Hello,Akina!" + "\033[0m");
        System.out.println("\033[47;4m" + "Hello,Akina!" + "\033[0m");
    }

}