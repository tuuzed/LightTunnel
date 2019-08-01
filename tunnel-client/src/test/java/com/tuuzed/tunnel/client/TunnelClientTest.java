package com.tuuzed.tunnel.client;

import com.tuuzed.tunnel.common.logging.LogConfigurator;
import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import com.tuuzed.tunnel.common.proto.ProtoRequest;
import com.tuuzed.tunnel.common.util.SslContexts;
import io.netty.handler.ssl.SslContext;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TunnelClientTest {
    private static final Logger logger = LoggerFactory.getLogger(TunnelClientTest.class);
    private TunnelClient manager;

    @Before
    public void setUp() {
        LogConfigurator.setLevel(Logger.ALL);
        manager = new TunnelClient.Builder()
            .setAutoReconnect(true)
            .setWorkerThreads(4)
            .setListener(new TunnelClientListener() {
                @Override
                public void onConnecting(@NotNull TunnelClient.Descriptor descriptor, boolean reconnect) {
                    logger.info("tunnel: {}, reconnect: {}", descriptor, reconnect);
                }

                @Override
                public void onConnected(@NotNull TunnelClient.Descriptor descriptor) {
                    logger.info("{}", descriptor);
                }

                @Override
                public void onDisconnect(@NotNull TunnelClient.Descriptor descriptor, boolean fatal) {
                    logger.info("tunnel: {}, deadly: {}", descriptor, fatal);
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
        final String serverAddr = "127.0.0.1";
        final int serverPort = 5001;
        final SslContext sslContext = SslContexts.forClient("../resources/jks/client.jks", "ctunnelpass");
        // error
        manager.connect(serverAddr, serverPort,
            ProtoRequest.tcpBuilder(65000)
                .setLocalAddr("192.168.1.1")
                .setLocalPort(80)
                .setOption("token", "tk123456")
                .build()
            ,
            sslContext
        );
        // replace
        final TunnelClient.Descriptor replaceDescriptor = manager.connect(serverAddr, serverPort,
            ProtoRequest.tcpBuilder(20000)
                .setLocalAddr("192.168.1.1")
                .setLocalPort(80)
                .setOption("token", "tk123456")
                .build()
            ,
            sslContext
        );
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(6_000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                replaceDescriptor.shutdown();
            }
        }).start();
        // tcp-http
        manager.connect(serverAddr, serverPort,
            ProtoRequest.tcpBuilder(10080)
                .setLocalAddr("192.168.1.1")
                .setLocalPort(80)
                .setOption("token", "tk123456")
                .build()
            ,
            sslContext);
        // vnc
        manager.connect(serverAddr, serverPort,
            ProtoRequest.tcpBuilder(15900)
                .setLocalAddr("192.168.1.33")
                .setLocalPort(5900)
                .setOption("token", "tk123456")
                .build()
            ,
            sslContext
        );
        // ssh
        manager.connect(serverAddr, serverPort,
            ProtoRequest.tcpBuilder(10022)
                .setLocalAddr("192.168.1.10")
                .setLocalPort(22)
                .setOption("token", "tk123456")
                .build()
            ,
            sslContext
        );
        // vhost-http
        manager.connect(serverAddr, serverPort,
            ProtoRequest.httpBuilder("t1.tunnel.lo")
                .setLocalAddr("192.168.1.1")
                .setLocalPort(80)
                .setOption("token", "tk123456")
                .build()
            ,
            sslContext
        );
        // vhost-http
        manager.connect(serverAddr, serverPort,
            ProtoRequest.httpBuilder("t2.tunnel.lo")
                .setLocalAddr("apache.org")
                .setLocalPort(80)
                .setOption("token", "tk123456")
                .build()
            ,
            sslContext
        );

        System.in.read();
    }
}