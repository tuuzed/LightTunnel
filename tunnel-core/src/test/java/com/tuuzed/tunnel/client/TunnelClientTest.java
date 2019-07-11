package com.tuuzed.tunnel.client;

import org.junit.Test;

public class TunnelClientTest {

    @Test
    public void start() throws Exception {
        new TunnelClient(
                "127.0.0.1", 4000,
                "192.168.1.1", 80,
                10080
        ).start(false);
        new TunnelClient(
                "127.0.0.1", 4000,
                "192.168.1.10", 22,
                10022
        ).start(false);
        System.in.read();
    }
}