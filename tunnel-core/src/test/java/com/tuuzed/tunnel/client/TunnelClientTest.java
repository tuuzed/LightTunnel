package com.tuuzed.tunnel.client;

import com.tuuzed.tunnel.common.protocol.OpenTunnelRequest;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class TunnelClientTest {

    @Test
    public void start() throws Exception {
        Map<String, String> arguments = new HashMap<>();
        arguments.put("token", "tk123456");
        // error
        TunnelClientManager.getInstance()
                .connect("127.0.0.1", 4000, new OpenTunnelRequest(OpenTunnelRequest.TYPE_TCP,
                        "192.168.1.1", 80,
                        65000,
                        arguments
                ));
        // replace
        final TunnelClient replaceTunnelClient = TunnelClientManager.getInstance()
                .connect("127.0.0.1", 4000, new OpenTunnelRequest(OpenTunnelRequest.TYPE_TCP,
                        "192.168.1.1", 80,
                        20000,
                        arguments
                ));
        // http
        TunnelClientManager.getInstance()
                .connect("127.0.0.1", 4000, new OpenTunnelRequest(OpenTunnelRequest.TYPE_TCP,
                        "192.168.1.1", 80,
                        10080,
                        arguments
                ));
        // vnc
        TunnelClientManager.getInstance()
                .connect("127.0.0.1", 4000, new OpenTunnelRequest(OpenTunnelRequest.TYPE_TCP,
                        "192.168.1.33", 5900,
                        15900,
                        arguments
                ));
        // ssh
        TunnelClientManager.getInstance()
                .connect("127.0.0.1", 4000, new OpenTunnelRequest(OpenTunnelRequest.TYPE_TCP,
                        "192.168.1.10", 22,
                        10022,
                        arguments
                ));
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(6_000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                replaceTunnelClient.shutdown();
            }
        }).start();
        System.in.read();
    }
}