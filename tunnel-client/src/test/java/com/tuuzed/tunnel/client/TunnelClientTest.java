package com.tuuzed.tunnel.client;

import com.tuuzed.tunnel.common.log4j.Log4jInitializer;
import com.tuuzed.tunnel.common.proto.ProtoRequest;
import com.tuuzed.tunnel.common.util.HttpUtils;
import com.tuuzed.tunnel.common.util.SslContexts;
import io.netty.handler.ssl.SslContext;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TunnelClientTest {
    private TunnelClient client;
    private ProtoRequest portError;
    private ProtoRequest tcpHttp;
    private ProtoRequest vnc;
    private ProtoRequest ssh;
    private ProtoRequest vhostHttp1;
    private ProtoRequest vhostHttp2;
    private ProtoRequest vhostHttps1;
    private ProtoRequest vhostHttps2;
    private ProtoRequest portReplaced;


    @Test
    public void start() throws Exception {
        final String serverAddr = "127.0.0.1";
        final int serverPort = 5001;
        final SslContext sslContext = SslContexts.forClient("../resources/jks/client.jks", "ctunnelpass");

        client.connect(serverAddr, serverPort, portError, sslContext);

        client.connect(serverAddr, serverPort, tcpHttp, sslContext);
        client.connect(serverAddr, serverPort, vnc, sslContext);
        client.connect(serverAddr, serverPort, ssh, sslContext);
        // vhostHttp
        client.connect(serverAddr, serverPort, vhostHttp1, sslContext);
        client.connect(serverAddr, serverPort, vhostHttp2, sslContext);
        // vhostHttps
        client.connect(serverAddr, serverPort, vhostHttps1, sslContext);
        client.connect(serverAddr, serverPort, vhostHttps2, sslContext);

        final TunnelClientDescriptor portReplacedDescriptor = client.connect(serverAddr, serverPort, portReplaced, sslContext);
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
        Thread.currentThread().join();
    }

    @Before
    public void setUp() {
        Log4jInitializer.initializeThirdLibrary(Level.WARN);
        Log4jInitializer.builder().initialize();

        client = TunnelClient.builder()
            .setAutoReconnect(true)
            .setWorkerThreads(4)
            .setListener(new TunnelClientListener() {
                private final Logger logger = LoggerFactory.getLogger(TunnelClientListener.class);

                @Override
                public void onConnecting(@NotNull TunnelClientDescriptor descriptor, boolean reconnect) {
                    logger.warn("tunnel: {}, reconnect: {}", descriptor, reconnect);
                }

                @Override
                public void onConnected(@NotNull TunnelClientDescriptor descriptor) {
                    logger.error("{}", descriptor);
                }

                @Override
                public void onDisconnect(@NotNull TunnelClientDescriptor descriptor, boolean fatal) {
                    logger.error("tunnel: {}, deadly: {}", descriptor, fatal);
                }
            })
            .build();

        portError = ProtoRequest.tcpBuilder(65000)
            .setLocalAddr("192.168.1.1")
            .setLocalPort(80)
            .setToken("tk123456")
            .build();

        portReplaced = ProtoRequest.tcpBuilder(20000) // 20080
            .setLocalAddr("192.168.1.1")
            .setLocalPort(80)
            .setToken("tk123456")
            .build();

        tcpHttp = ProtoRequest.tcpBuilder(10080) // 20080
            .setLocalAddr("192.168.1.1")
            .setLocalPort(80)
            .setOption("token", "tk123456")
            .build();

        vnc = ProtoRequest.tcpBuilder(15900) // 20080
            .setLocalAddr("192.168.1.33")
            .setLocalPort(5900)
            .setToken("tk123456")
            .build();

        ssh = ProtoRequest.tcpBuilder(10022) // 20080
            .setLocalAddr("192.168.1.10")
            .setLocalPort(22)
            .setToken("tk123456")
            .build();

        vhostHttp1 = ProtoRequest.httpBuilder("t1.tunnel.lo")
            .setLocalAddr("192.168.1.1")
            .setLocalPort(80)
            .setToken("tk123456")
            .setRewriteHeaders(HttpUtils.headersOf("X-Real-IP", "$remote_addr"))
            .setWriteHeaders(HttpUtils.headersOf("X-User-Agent", "Tunnel"))
            .build();

        vhostHttp2 = ProtoRequest.httpBuilder("t2.tunnel.lo")
            .setLocalAddr("111.230.198.37")
            .setLocalPort(10080)
            .setToken("tk123456")
            .setRewriteHeaders(HttpUtils.headersOf("X-Real-IP", "$remote_addr"))
            .setWriteHeaders(HttpUtils.headersOf("X-User-Agent", "Tunnel"))
            .setBasicAuth(true, "OCR")
            .setBasicAuthAccount("admin", "admin")
            .build();


        vhostHttps1 = ProtoRequest.httpsBuilder("t1.tunnel.lo")
            .setLocalAddr("192.168.1.1")
            .setLocalPort(80)
            .setToken("tk123456")
            .setRewriteHeaders(HttpUtils.headersOf("X-Real-IP", "$remote_addr"))
            .setWriteHeaders(HttpUtils.headersOf("X-User-Agent", "Tunnel"))
            .build();

        vhostHttps2 = ProtoRequest.httpsBuilder("t2.tunnel.lo")
            .setLocalAddr("111.230.198.37")
            .setLocalPort(10080)
            .setToken("tk123456")
            .setRewriteHeaders(HttpUtils.headersOf("X-Real-IP", "$remote_addr"))
            .setWriteHeaders(HttpUtils.headersOf("X-User-Agent", "Tunnel"))
            .setBasicAuth(true, "OCR")
            .setBasicAuthAccount("admin", "admin")
            .build();
    }

    @After
    public void shutDown() {
        client.destroy();
    }


}