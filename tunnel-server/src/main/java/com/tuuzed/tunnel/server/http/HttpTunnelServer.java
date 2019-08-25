package com.tuuzed.tunnel.server.http;

import com.tuuzed.tunnel.interceptor.HttpRequestInterceptor;
import com.tuuzed.tunnel.server.internal.ServerTunnelSessions;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpTunnelServer {
    private static final Logger logger = LoggerFactory.getLogger(HttpTunnelServer.class);

    private final boolean https;
    @Nullable
    private final String bindAddr;
    private final int bindPort;
    @NotNull
    private final HttpTunnelRegistry registry;
    @NotNull
    private final ServerBootstrap serverBootstrap;


    public HttpTunnelServer(
        @NotNull final NioEventLoopGroup bossGroup,
        @NotNull final NioEventLoopGroup workerGroup,
        @Nullable String bindAddr,
        int bindPort,
        @Nullable final SslContext sslContext,
        @NotNull final HttpRequestInterceptor interceptor
    ) {
        this.https = sslContext != null;
        this.bindAddr = bindAddr;
        this.bindPort = bindPort;
        this.registry = new HttpTunnelRegistry();
        this.serverBootstrap = new ServerBootstrap();
        this.serverBootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .childOption(ChannelOption.AUTO_READ, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(new HttpTunnelServerChannelInitializer(sslContext, registry, interceptor));
    }

    @NotNull
    public HttpTunnelRegistry registry() {
        return registry;
    }

    public void start() throws Exception {
        if (bindAddr != null) {
            serverBootstrap.bind(bindAddr, bindPort).get();
        } else {
            serverBootstrap.bind(bindPort).get();
        }
        logger.info("Serving {} on {} port {}",
            https ? "https" : "http",
            (bindAddr == null) ? "any address" : bindAddr,
            bindPort
        );
    }

    @Nullable
    public Channel getSessionChannel(long tunnelToken, long sessionToken) {
        return registry.getSessionChannel(tunnelToken, sessionToken);
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
