package com.tuuzed.tunnel.client;

import com.tuuzed.tunnel.common.logging.LogConfigurator;
import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import com.tuuzed.tunnel.common.protocol.OpenTunnelRequest;
import com.tuuzed.tunnel.common.ssl.SslContexts;
import io.netty.handler.ssl.SslContext;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class TunnelClientTest {
    private static final Logger logger = LoggerFactory.getLogger(TunnelClientTest.class);
    private TunnelClient manager;

    @Before
    public void setUp() {
        LogConfigurator.setLevel(Logger.INFO);
        manager = new TunnelClient.Builder()
                .setAutoReconnect(true)
                .setWorkerThreads(1)
                .setListener(new TunnelClient.Listener() {
                    @Override
                    public void onConnecting(@NotNull TunnelClient.Tunnel tunnel, boolean reconnect) {
                        logger.info("tunnel: {}, reconnect: {}", tunnel, reconnect);
                    }

                    @Override
                    public void onConnected(@NotNull TunnelClient.Tunnel tunnel) {
                        logger.info("{}", tunnel);
                    }

                    @Override
                    public void onDisconnect(@NotNull TunnelClient.Tunnel tunnel, boolean deadly) {
                        logger.info("tunnel: {}, deadly: {}", tunnel, deadly);
                    }
                })
                .build();
    }

    @After
    public void shutDown() {
        manager.destroy();
    }

    @Test
    public void start() throws Exception {
        Map<String, String> arguments = new HashMap<>();
        arguments.put("token", "tk123456");
        String serverAddr = "127.0.0.1";
        int serverPort = 5001;
        SslContext context = SslContexts.forClient("../jks/tunnel-client.jks", "ctunnelpass");
        // error
        manager.connect(serverAddr, serverPort,
                new OpenTunnelRequest(OpenTunnelRequest.TYPE_TCP,
                        "192.168.1.1", 80,
                        65000,
                        arguments
                ),
                context
        );
        // replace
        final TunnelClient.Tunnel replaceTunnel = manager.connect(serverAddr, serverPort,
                new OpenTunnelRequest(OpenTunnelRequest.TYPE_TCP,
                        "192.168.1.1", 80,
                        20000,
                        arguments
                ),
                context
        );
        // http
        manager.connect(serverAddr, serverPort,
                new OpenTunnelRequest(OpenTunnelRequest.TYPE_TCP,
                        "192.168.1.1", 80,
                        10080,
                        arguments
                ),
                context);
        // vnc
        manager.connect(serverAddr, serverPort,
                new OpenTunnelRequest(OpenTunnelRequest.TYPE_TCP,
                        "192.168.1.33", 5900,
                        15900,
                        arguments
                ),
                context
        );
        // ssh
        manager.connect(serverAddr, serverPort,
                new OpenTunnelRequest(OpenTunnelRequest.TYPE_TCP,
                        "192.168.1.10", 22,
                        10022,
                        arguments
                ),
                context
        );
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(6_000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                replaceTunnel.shutdown();
            }
        }).start();
        System.in.read();
    }
}