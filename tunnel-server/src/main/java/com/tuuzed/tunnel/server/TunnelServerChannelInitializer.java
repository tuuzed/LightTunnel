package com.tuuzed.tunnel.server;

import com.tuuzed.tunnel.common.interceptor.ProtoRequestInterceptor;
import com.tuuzed.tunnel.common.proto.ProtoHeartbeatHandler;
import com.tuuzed.tunnel.common.proto.ProtoMessageDecoder;
import com.tuuzed.tunnel.common.proto.ProtoMessageEncoder;
import com.tuuzed.tunnel.server.http.HttpServer;
import com.tuuzed.tunnel.server.internal.TokenProducer;
import com.tuuzed.tunnel.server.tcp.TcpServer;
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
    private final TcpServer tcpServer;
    @Nullable
    private final HttpServer httpServer;
    @Nullable
    private final HttpServer httpsServer;
    @NotNull
    private final ProtoRequestInterceptor protoRequestInterceptor;
    @NotNull
    private final TokenProducer tunnelTokenProducer;

    public TunnelServerChannelInitializer(
        @Nullable SslContext sslContext,
        @NotNull TcpServer tcpServer,
        @Nullable HttpServer httpServer,
        @Nullable HttpServer httpsServer,
        @NotNull ProtoRequestInterceptor protoRequestInterceptor,
        @NotNull TokenProducer tunnelTokenProducer
    ) {
        this.sslContext = sslContext;
        this.tcpServer = tcpServer;
        this.httpServer = httpServer;
        this.httpsServer = httpsServer;
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
                tcpServer, httpServer, httpsServer, protoRequestInterceptor, tunnelTokenProducer
            ))
        ;
    }
}
