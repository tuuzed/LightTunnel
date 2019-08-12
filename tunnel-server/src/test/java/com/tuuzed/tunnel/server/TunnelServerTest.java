package com.tuuzed.tunnel.server;

import com.tuuzed.tunnel.common.logging.LogAdapter;
import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import com.tuuzed.tunnel.common.proto.ProtoRequestInterceptor;
import com.tuuzed.tunnel.common.util.SslContexts;
import com.tuuzed.tunnel.server.http.HttpRequestInterceptor;
import io.netty.handler.ssl.SslContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TunnelServerTest {

    private TunnelServer tunnelServer;

    @Before
    public void setUp() throws Exception {
        LogAdapter logcat = LoggerFactory.getLogAdapter(LoggerFactory.LOGCAT);
        if (logcat != null) {
            logcat.setLevel(Logger.ALL);
        }
        ProtoRequestInterceptor protoRequestInterceptor = new ProtoRequestInterceptorImpl();
        HttpRequestInterceptor httpRequestInterceptor = new HttpRequestInterceptorImpl();
        SslContext sslContext = SslContexts.forServer(
            "../resources/jks/server.jks",
            "stunnelpass",
            "stunnelpass"
        );
        this.tunnelServer = TunnelServer.builder()
            .setBossThreads(1)
            .setWorkerThreads(-1)
            .setBindPort(5000)
            .enableSsl(sslContext, 5001)
            .setHttpBindPort(5080)
            .setProtoRequestInterceptor(protoRequestInterceptor)
            .setHttpRequestInterceptor(httpRequestInterceptor)
            .build();
    }

    @After
    public void shutDown() {
        this.tunnelServer.destroy();
    }

    @Test
    public void start() throws Exception {
        tunnelServer.start();
        System.in.read();
    }

}