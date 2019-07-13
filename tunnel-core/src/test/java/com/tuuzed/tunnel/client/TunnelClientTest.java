package com.tuuzed.tunnel.client;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class TunnelClientTest {

    @Test
    public void start() throws Exception {
        Map<String, String> options = new HashMap<>();
        options.put("token", "tk123456");
        new TunnelClient(
                "127.0.0.1", 4000,
                "127.0.0.1", 80,
                10080,
                options
        ).start();
//        new TunnelClient(
//                "127.0.0.1", 4000,
//                "192.168.1.33", 5900,
//                15900,
//                options
//        ).start();
        new TunnelClient(
                "127.0.0.1", 4000,
                "139.199.221.244", 22,
                10022,
                options
        ).start();
        System.in.read();
    }
}