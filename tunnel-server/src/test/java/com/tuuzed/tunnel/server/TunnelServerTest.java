package com.tuuzed.tunnel.server;

import com.tuuzed.tunnel.common.interceptor.SimpleRequestInterceptor;
import com.tuuzed.tunnel.common.log4j.Log4jInitializer;
import com.tuuzed.tunnel.common.util.SslContexts;
import io.netty.handler.ssl.SslContext;
import org.apache.log4j.Level;
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
        Log4jInitializer.initializeThirdLibrary(Level.WARN);
        // 设置控制台日志
        Log4jInitializer.builder()
            .setConsole(true)
            .setLevel(Level.ALL)
            .initialize();
        // 配置文件日志
        Log4jInitializer.builder()
            .setConsole(false)
            .setFile("../logs/tunnel-server.log")
            .setLevel(Level.ALL)
            .initialize();

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