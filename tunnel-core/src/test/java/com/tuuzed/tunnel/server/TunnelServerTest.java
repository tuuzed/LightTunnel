package com.tuuzed.tunnel.server;

import org.junit.Test;

public class TunnelServerTest {
    @Test
    public void start() throws Exception {
        new TunnelServer(null, 4000).start();
    }

}