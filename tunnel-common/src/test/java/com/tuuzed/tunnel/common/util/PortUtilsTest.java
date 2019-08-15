package com.tuuzed.tunnel.common.util;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PortUtilsTest {

    @Test
    public void inPortRule() {
        assertFalse(PortUtils.inPortRule("10000-21000", 40000));
        assertFalse(PortUtils.inPortRule("10000-21000,30000", 40000));
        assertFalse(PortUtils.inPortRule("10000-21000,30000", 1024));
        assertTrue(PortUtils.inPortRule("10000-21000,30000", 10002));
        assertTrue(PortUtils.inPortRule("10000-21000,30000", 10000));
        assertTrue(PortUtils.inPortRule("10000-21000,30000", 21000));
        assertTrue(PortUtils.inPortRule("10000-21000,30000", 30000));
    }
}