package com.tuuzed.tunnel.server.tcp;

import com.tuuzed.tunnel.server.internal.ServerTunnelSessions;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TcpServer {
    @NotNull
    private final TcpTunnelRegistry registry;
    @NotNull
    private final TcpTunnelStats stats;
    @NotNull
    private final ServerBootstrap serverBootstrap;

    public TcpServer(
        @NotNull final NioEventLoopGroup bossGroup,
        @NotNull final NioEventLoopGroup workerGroup
    ) {
        this.registry = new TcpTunnelRegistry();
        this.stats = new TcpTunnelStats();
        this.serverBootstrap = new ServerBootstrap();
        this.serverBootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .childOption(ChannelOption.AUTO_READ, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(new TcpServerChannelInitializer(registry, stats));
    }

    @NotNull
    public TcpTunnelRegistry registry() {
        return registry;
    }

    @NotNull
    public TcpTunnelStats stats() {
        return stats;
    }

    public void startTunnel(@Nullable String addr, int port, @NotNull ServerTunnelSessions tunnelSessions) throws Exception {
        final TcpTunnelDescriptor descriptor = new TcpTunnelDescriptor(addr, port, tunnelSessions);
        descriptor.open(serverBootstrap);
        registry.register(port, tunnelSessions, descriptor);
    }

    public void shutdownTunnel(long tunnelToken) {
        registry.unregister(tunnelToken);
    }

    @Nullable
    public Channel getSessionChannel(long tunnelToken, long sessionToken) {
        return registry.getSessionChannel(tunnelToken, sessionToken);
    }

    public void destroy() {
        registry.destroy();
    }


}
