package com.tuuzed.tunnel.util;

import org.junit.Test;

public class SslContextsTest {

    @Test
    public void forServer() {
        try {
            SslContexts.forServer("../resources/jks/tunnels.jks", "stunnelpass", "stunnelpass");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void forClient() {
        try {
            SslContexts.forClient("../resources/jks/tunnelc.jks", "ctunnelpass");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}