package com.tuuzed.tunnel.server.tcp;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import org.jetbrains.annotations.NotNull;


public class TcpServerChannelInitializer extends ChannelInitializer<SocketChannel> {

    @NotNull
    private final TcpTunnelRegistry tcpTunnelRegistry;
    @NotNull
    private final TcpTunnelStats tcpTunnelStats;

    public TcpServerChannelInitializer(
        @NotNull TcpTunnelRegistry registry,
        @NotNull TcpTunnelStats stats
    ) {
        this.tcpTunnelRegistry = registry;
        this.tcpTunnelStats = stats;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline()
            .addFirst(new TcpTunnelStatsHandler(tcpTunnelStats))
            .addLast(new TcpServerChannelHandler(tcpTunnelRegistry))
        ;
    }
}
