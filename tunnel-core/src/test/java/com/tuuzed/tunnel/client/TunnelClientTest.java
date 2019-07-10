package com.tuuzed.tunnel.client;

import org.junit.Test;

public class TunnelClientTest {

    @Test
    public void start() throws Exception {
        new TunnelClient("127.0.0.1", 4000, "127.0.0.1", 80, 10080).start();
        new TunnelClient("127.0.0.1", 4000, "tuuzed.com", 22, 10022).start();
        System.in.read();
    }
}