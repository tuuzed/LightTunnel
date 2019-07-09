package com.tuuzed.tunnel.server;

import org.junit.Test;

public class TunnelServerTest {
    @Test
    public void start() throws Exception {
        new TunnelServer().start("0.0.0.0", 4000);
    }

}