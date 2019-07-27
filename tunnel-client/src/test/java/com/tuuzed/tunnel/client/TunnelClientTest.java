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
        LogConfigurator.setLevel(Logger.ALL);
        manager = new TunnelClient.Builder()
                .setAutoReconnect(true)
                .setWorkerThreads(4)
                .setListener(new TunnelClient.Listener() {
                    @Override
                    public void onConnecting(@NotNull TunnelClient.TunnelDescriptor tunnelDescriptor, boolean reconnect) {
                        logger.info("tunnel: {}, reconnect: {}", tunnelDescriptor, reconnect);
                    }

                    @Override
                    public void onConnected(@NotNull TunnelClient.TunnelDescriptor tunnel) {
                        logger.info("{}", tunnel);
                    }

                    @Override
                    public void onDisconnect(@NotNull TunnelClient.TunnelDescriptor tunnelDescriptor, boolean deadly) {
                        logger.info("tunnel: {}, deadly: {}", tunnelDescriptor, deadly);
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
                OpenTunnelRequest.tcpBuilder(65000)
                        .setLocalAddr("192.168.1.1")
                        .setLocalPort(80)
                        .setOption("token", "tk123456")
                        .build()
                ,
                context
        );
        // replace
        final TunnelClient.TunnelDescriptor replaceTunnelDescriptor = manager.connect(serverAddr, serverPort,
                OpenTunnelRequest.tcpBuilder(20000)
                        .setLocalAddr("192.168.1.1")
                        .setLocalPort(80)
                        .setOption("token", "tk123456")
                        .build()
                ,
                context
        );
        // http
        manager.connect(serverAddr, serverPort,
                OpenTunnelRequest.tcpBuilder(10080)
                        .setLocalAddr("192.168.1.1")
                        .setLocalPort(80)
                        .setOption("token", "tk123456")
                        .build()
                ,
                context);
        // vnc
        manager.connect(serverAddr, serverPort,
                OpenTunnelRequest.tcpBuilder(15900)
                        .setLocalAddr("192.168.1.33")
                        .setLocalPort(5900)
                        .setOption("token", "tk123456")
                        .build()
                ,
                context
        );
        // ssh
        manager.connect(serverAddr, serverPort,
                OpenTunnelRequest.tcpBuilder(10022)
                        .setLocalAddr("192.168.1.10")
                        .setLocalPort(22)
                        .setOption("token", "tk123456")
                        .build()
                ,
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
                replaceTunnelDescriptor.shutdown();
            }
        }).start();
        System.in.read();
    }
}