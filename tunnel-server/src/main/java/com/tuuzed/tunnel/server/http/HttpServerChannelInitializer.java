package com.tuuzed.tunnel.server.http;

import com.tuuzed.tunnel.common.interceptor.HttpRequestInterceptor;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HttpServerChannelInitializer extends ChannelInitializer<SocketChannel> {

    @Nullable
    private final SslContext sslContext;
    @NotNull
    private final HttpTunnelRegistry httpTunnelRegistry;
    @NotNull
    private final HttpRequestInterceptor interceptor;

    public HttpServerChannelInitializer(
        @Nullable SslContext sslContext,
        @NotNull HttpTunnelRegistry registry,
        @NotNull HttpRequestInterceptor interceptor
    ) {
        this.sslContext = sslContext;
        this.httpTunnelRegistry = registry;
        this.interceptor = interceptor;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        if (sslContext != null) { // 启用SSL
            ch.pipeline().addFirst(new SslHandler(sslContext.newEngine(ch.alloc())));
        }
        ch.pipeline()
            .addLast(new HttpRequestDecoder())
            .addLast(new HttpServerChannelHandler(httpTunnelRegistry, interceptor))
        ;
    }
}
