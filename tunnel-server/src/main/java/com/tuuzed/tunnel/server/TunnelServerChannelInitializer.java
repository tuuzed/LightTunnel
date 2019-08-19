package com.tuuzed.tunnel.server;

import com.tuuzed.tunnel.common.interceptor.ProtoRequestInterceptor;
import com.tuuzed.tunnel.common.proto.ProtoHeartbeatHandler;
import com.tuuzed.tunnel.common.proto.ProtoMessageDecoder;
import com.tuuzed.tunnel.common.proto.ProtoMessageEncoder;
import com.tuuzed.tunnel.server.http.HttpTunnelServer;
import com.tuuzed.tunnel.server.internal.TokenProducer;
import com.tuuzed.tunnel.server.tcp.TcpTunnelServer;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TunnelServerChannelInitializer extends ChannelInitializer<SocketChannel> {

    @Nullable
    private final SslContext sslContext;
    @NotNull
    private final TcpTunnelServer tcpTunnelServer;
    @Nullable
    private final HttpTunnelServer httpTunnelServer;
    @Nullable
    private final HttpTunnelServer httpsTunnelServer;
    @NotNull
    private final ProtoRequestInterceptor protoRequestInterceptor;
    @NotNull
    private final TokenProducer tunnelTokenProducer;

    public TunnelServerChannelInitializer(
        @Nullable SslContext sslContext,
        @NotNull TcpTunnelServer tcpTunnelServer,
        @Nullable HttpTunnelServer httpTunnelServer,
        @Nullable HttpTunnelServer httpsTunnelServer,
        @NotNull ProtoRequestInterceptor protoRequestInterceptor,
        @NotNull TokenProducer tunnelTokenProducer
    ) {
        this.sslContext = sslContext;
        this.tcpTunnelServer = tcpTunnelServer;
        this.httpTunnelServer = httpTunnelServer;
        this.httpsTunnelServer = httpsTunnelServer;
        this.protoRequestInterceptor = protoRequestInterceptor;
        this.tunnelTokenProducer = tunnelTokenProducer;
    }


    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        if (sslContext != null) { // 启用SSL
            ch.pipeline().addFirst(new SslHandler(sslContext.newEngine(ch.alloc())));
        }
        ch.pipeline()
            .addLast(new ProtoMessageDecoder())
            .addLast(new ProtoMessageEncoder())
            .addLast(new ProtoHeartbeatHandler())
            .addLast(new TunnelServerChannelHandler(
                tcpTunnelServer, httpTunnelServer, httpsTunnelServer,
                protoRequestInterceptor, tunnelTokenProducer
            ))
        ;
    }
}
