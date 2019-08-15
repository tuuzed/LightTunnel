package com.tuuzed.tunnel.common.logging;

import org.junit.Test;

import static org.junit.Assert.*;

public class UtilsTest {

    @Test
    public void getLevelColor() {
        System.out.println(Utils.getLevelColor(Logger.DEBUG));
    }

    @Test
    public void printlnColored() {
        Utils.printlnColored("Hello", 37, -1, -1, System.err);
    }
}