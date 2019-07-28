package com.tuuzed.tunnel.server;

import com.tuuzed.tunnel.common.proto.ProtoException;
import com.tuuzed.tunnel.common.proto.ProtoRequest;
import com.tuuzed.tunnel.common.util.PortUtils;
import com.tuuzed.tunnel.common.util.SslContexts;
import io.netty.handler.ssl.SslContext;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

public class TunnelServerTest {

    private TunnelServer tunnelServer;

    @Before
    public void setUp() throws Exception {
        ProtoRequest.Interceptor interceptor = new ProtoRequest.Interceptor() {
            @NotNull
            @Override
            public ProtoRequest proceed(@NotNull ProtoRequest request) throws ProtoException {
                if (request.isTcp()) {
                    final String token = request.option("token");
                    final int remotePort = request.remotePort();
                    if (!"tk123456".equals(token)) {
                        throw new ProtoException(String.format("request(%s), Bad Token(%s)", request.toString(), token));
                    }
                    if (!PortUtils.inPortRule("1024-60000", remotePort)) {
                        throw new ProtoException(String.format("request(%s), remotePort(%s) Not allowed to use.", request.toString(), remotePort));
                    }
                    if (remotePort == 20000) {
                        // 替换远程端口
                        return request.newTcpBuilder(20080).build();
                    }
                    return request;
                }
                return request;
            }
        };
        SslContext context = SslContexts.forServer(
            "../resources/jks/tunnel-server.jks", "stunnelpass", "stunnelpass"
        );
        this.tunnelServer = new TunnelServer.Builder()
            .setBossThreads(1)
            .setWorkerThreads(Runtime.getRuntime().availableProcessors() * 2 + 1)
            .setBindPort(5000)
            .enableSsl(context, 5001)
            .setHttpBindPort(5080)
            .setProtoRequestInterceptor(interceptor)
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

    @Test
    public void host() {
        String host = "t1.tunnel.lo:5080:11";
        System.out.println(Arrays.toString(host.split(":")));
    }
}