package com.tuuzed.tunnel.server;

import com.tuuzed.tunnel.common.interceptor.SimpleRequestInterceptor;
import com.tuuzed.tunnel.common.logging.LogAdapter;
import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import com.tuuzed.tunnel.common.util.SslContexts;
import io.netty.handler.ssl.SslContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TunnelServerTest {

    private TunnelServer tunnelServer;

    @Test
    public void start() throws Exception {
        tunnelServer.start();
        System.in.read();
    }

    @Before
    public void setUp() throws Exception {
        LogAdapter logcat = LoggerFactory.getLogAdapter(LoggerFactory.LOGCAT);
        if (logcat != null) {
            logcat.setLevel(Logger.ALL);
        }
        SimpleRequestInterceptor simpleRequestInterceptor = new SimpleRequestInterceptor(
            "tk123456", "1024-60000"
        );
        SslContext sslContext = SslContexts.forServer(
            "../resources/jks/server.jks",
            "stunnelpass",
            "stunnelpass"
        );
        this.tunnelServer = TunnelServer.builder()
            .setBossThreads(1)
            .setWorkerThreads(-1)
            .setProtoRequestInterceptor(simpleRequestInterceptor)

            .setBindAddr(null)
            .setBindPort(5000)

            .setSslEnable(true)
            .setSslContext(sslContext)
            .setSslBindAddr(null)
            .setSslBindPort(5001)

            .setHttpEnable(true)
            .setHttpBindAddr(null)
            .setHttpBindPort(5080)
            .setHttpRequestInterceptor(simpleRequestInterceptor)

            .setHttpsEnable(true)
            .setHttpsContext(sslContext)
            .setHttpsBindAddr(null)
            .setHttpsBindPort(5443)
            .setHttpsRequestInterceptor(simpleRequestInterceptor)

            .build();
    }

    @After
    public void shutDown() {
        this.tunnelServer.destroy();
    }


}