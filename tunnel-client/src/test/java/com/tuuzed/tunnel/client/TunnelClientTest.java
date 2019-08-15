package com.tuuzed.tunnel.client;

import com.tuuzed.tunnel.common.logging.LogAdapter;
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
    private TunnelClient client;

    @Before
    public void setUp() {
        LogAdapter logcat = LoggerFactory.getLogAdapter(LoggerFactory.LOGCAT);
        if (logcat != null) {
            logcat.setLevel(Logger.ALL);
        }
        client = TunnelClient.builder()
            .setAutoReconnect(true)
            .setWorkerThreads(4)
            .setListener(new TunnelClientListener() {
                @Override
                public void onConnecting(@NotNull TunnelClientDescriptor descriptor, boolean reconnect) {
                    logger.info("tunnel: {}, reconnect: {}", descriptor, reconnect);
                }

                @Override
                public void onConnected(@NotNull TunnelClientDescriptor descriptor) {
                    logger.info("{}", descriptor);
                }

                @Override
                public void onDisconnect(@NotNull TunnelClientDescriptor descriptor, boolean fatal) {
                    logger.info("tunnel: {}, deadly: {}", descriptor, fatal);
                }
            })
            .build();
    }

    @After
    public void shutDown() {
        client.destroy();
    }

    @Test
    public void start() throws Exception {
        final String serverAddr = "127.0.0.1";
        final int serverPort = 5001;
        final SslContext sslContext = SslContexts.forClient("../resources/jks/client.jks", "ctunnelpass");


        ProtoRequest portError = ProtoRequest.tcpBuilder(65000)
            .setLocalAddr("192.168.1.1")
            .setLocalPort(80)
            .setToken("tk123456")
            .build();

        ProtoRequest portReplaced = ProtoRequest.tcpBuilder(20000) // 20080
            .setLocalAddr("192.168.1.1")
            .setLocalPort(80)
            .setToken("tk123456")
            .build();

        ProtoRequest tcpHttp = ProtoRequest.tcpBuilder(10080) // 20080
            .setLocalAddr("192.168.1.1")
            .setLocalPort(80)
            .setOption("token", "tk123456")
            .build();

        ProtoRequest vnc = ProtoRequest.tcpBuilder(15900) // 20080
            .setLocalAddr("192.168.1.33")
            .setLocalPort(5900)
            .setToken("tk123456")
            .build();

        ProtoRequest ssh = ProtoRequest.tcpBuilder(10022) // 20080
            .setLocalAddr("192.168.1.10")
            .setLocalPort(22)
            .setToken("tk123456")
            .build();

        ProtoRequest vhostHttp1 = ProtoRequest.httpBuilder("t1.tunnel.lo")
            .setLocalAddr("192.168.1.1")
            .setLocalPort(80)
            .setToken("tk123456")
            .setSetHeaders("X-Real-IP:$remote_addr;Host:192.168.1.1")
            .setAddHeaders("X-User-Agent:Tunnel")
            .build();

        ProtoRequest vhostHttp2 = ProtoRequest.httpBuilder("t2.tunnel.lo")
            .setLocalAddr("111.230.198.37")
            .setLocalPort(10080)
            .setToken("tk123456")
            .setSetHeaders("X-Real-IP:$remote_addr;Host:111.230.198.37")
            .setAddHeaders("X-User-Agent:Tunnel")
            .build();


        ProtoRequest vhostHttps1 = ProtoRequest.httpsBuilder("t1.tunnel.lo")
            .setLocalAddr("192.168.1.1")
            .setLocalPort(80)
            .setToken("tk123456")
            .setSetHeaders("X-Real-IP:$remote_addr;Host:192.168.1.1")
            .setAddHeaders("X-User-Agent:Tunnel")
            .build();

        ProtoRequest vhostHttps2 = ProtoRequest.httpsBuilder("t2.tunnel.lo")
            .setLocalAddr("111.230.198.37")
            .setLocalPort(10080)
            .setToken("tk123456")
            .setSetHeaders("X-Real-IP:$remote_addr;Host:111.230.198.37")
            .setAddHeaders("X-User-Agent:Tunnel")
            .build();


        client.connect(serverAddr, serverPort, portError, sslContext);
        final TunnelClientDescriptor portReplacedDescriptor =
            client.connect(serverAddr, serverPort, portReplaced, sslContext);

        client.connect(serverAddr, serverPort, tcpHttp, sslContext);
        client.connect(serverAddr, serverPort, vnc, sslContext);
        client.connect(serverAddr, serverPort, ssh, sslContext);
        // vhostHttp
        client.connect(serverAddr, serverPort, vhostHttp1, sslContext);
        client.connect(serverAddr, serverPort, vhostHttp2, sslContext);
        // vhostHttps
        client.connect(serverAddr, serverPort, vhostHttps1, sslContext);
        client.connect(serverAddr, serverPort, vhostHttps2, sslContext);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(6_000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                portReplacedDescriptor.shutdown();
            }
        }).start();
        System.in.read();
    }
}