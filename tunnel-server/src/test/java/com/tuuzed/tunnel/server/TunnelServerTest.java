package com.tuuzed.tunnel.server;

import com.tuuzed.tunnel.common.protocol.OpenTunnelRequest;
import com.tuuzed.tunnel.common.protocol.OpenTunnelRequestInterceptor;
import com.tuuzed.tunnel.common.protocol.TunnelProtocolException;
import com.tuuzed.tunnel.common.ssl.SslContexts;
import com.tuuzed.tunnel.common.util.PortUtils;
import io.netty.handler.ssl.SslContext;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TunnelServerTest {

    private TunnelServer tunnelServer;

    @Before
    public void setUp() throws Exception {
        OpenTunnelRequestInterceptor interceptor = new OpenTunnelRequestInterceptor() {
            @NotNull
            @Override
            public OpenTunnelRequest proceed(@NotNull OpenTunnelRequest request) throws TunnelProtocolException {
                final String token = request.getOption("token");
                final int remotePort = request.getRemotePort();
                if (!"tk123456".equals(token)) {
                    throw new TunnelProtocolException(String.format("request(%s), Bad Token(%s)", request.toString(), token));
                }
                if (!PortUtils.inPortRule("1024-60000", remotePort)) {
                    throw new TunnelProtocolException(String.format("request(%s), remotePort(%s) Not allowed to use.", request.toString(), remotePort));
                }
                if (remotePort == 20000) {
                    // 替换远程端口
                    return request.newTcpBuilder(20080).build();
                }
                return request;
            }
        };
        SslContext context = SslContexts.forServer("../jks/tunnel-server.jks", "stunnelpass", "stunnelpass");
        this.tunnelServer = new TunnelServer.Builder()
                .setBindPort(5000)
                .setInterceptor(interceptor)
                .enableSsl(context, 5001)
                .setBossThreads(2)
                .setWorkerThreads(Runtime.getRuntime().availableProcessors() * 2 + 1)
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