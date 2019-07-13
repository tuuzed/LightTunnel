package com.tuuzed.tunnel.client;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class TunnelClientTest {

    @Test
    public void start() throws Exception {
        Map<String, String> arguments = new HashMap<>();
        arguments.put("token", "tk123456");
        // http
        new TunnelClient(
                "127.0.0.1", 4000,
                "192.168.1.1", 80,
                10080,
                arguments
        ).start();
        // vnc
//        new TunnelClient(
//                "127.0.0.1", 4000,
//                "192.168.1.33", 5900,
//                15900,
//                arguments
//        ).start();
        // ssh
        new TunnelClient(
                "127.0.0.1", 4000,
                "192.168.1.10", 22,
                10022,
                arguments
        ).start();
        System.in.read();
    }
}