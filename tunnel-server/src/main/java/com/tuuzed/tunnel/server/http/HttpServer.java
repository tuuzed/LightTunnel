package com.tuuzed.tunnel.server.http;

import com.tuuzed.tunnel.common.interceptor.HttpRequestInterceptor;
import com.tuuzed.tunnel.server.internal.ServerTunnelSessions;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HttpServer {
    @NotNull
    private final HttpTunnelRegistry registry;
    @NotNull
    private final ServerBootstrap serverBootstrap;

    public HttpServer(
        @NotNull final NioEventLoopGroup bossGroup,
        @NotNull final NioEventLoopGroup workerGroup,
        @Nullable final SslContext sslContext,
        @NotNull final HttpRequestInterceptor interceptor
    ) {
        this.registry = new HttpTunnelRegistry();
        this.serverBootstrap = new ServerBootstrap();
        this.serverBootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .childOption(ChannelOption.AUTO_READ, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    if (sslContext != null) { // 启用SSL
                        ch.pipeline().addFirst(new SslHandler(sslContext.newEngine(ch.alloc())));
                    }
                    ch.pipeline()
                        .addLast(new HttpRequestDecoder())
                        .addLast(new HttpServerChannelHandler(HttpServer.this, interceptor))
                    ;
                }
            });
    }

    @NotNull
    public HttpTunnelRegistry registry() {
        return registry;
    }

    public void serve(@Nullable String bindAddr, int bindPort) throws Exception {
        if (bindAddr != null) {
            serverBootstrap.bind(bindAddr, bindPort).get();
        } else {
            serverBootstrap.bind(bindPort).get();
        }
    }

    @Nullable
    public Channel getSessionChannel(long tunnelToken, long sessionToken) {
        HttpTunnelDescriptor descriptor = getDescriptorByTunnelToken(tunnelToken);
        if (descriptor == null) {
            return null;
        }
        return descriptor.tunnelSessions().getSessionChannel(sessionToken);
    }

    @Nullable
    public HttpTunnelDescriptor getDescriptorByTunnelToken(long tunnelToken) {
        return registry.getDescriptorByTunnelToken(tunnelToken);
    }

    @Nullable
    public HttpTunnelDescriptor getDescriptorByVhost(@NotNull String vhost) {
        return registry.getDescriptorByVhost(vhost);
    }

    public boolean isRegistered(@NotNull String vhost) {
        return registry.isRegistered(vhost);
    }

    public void register(@NotNull String vhost, @NotNull ServerTunnelSessions tunnelSessions) throws Exception {
        registry.register(vhost, tunnelSessions);
    }


    public void unregister(@Nullable String vhost) {
        registry.unregister(vhost);
    }

    public void destroy() {
        registry.destroy();
    }

}
