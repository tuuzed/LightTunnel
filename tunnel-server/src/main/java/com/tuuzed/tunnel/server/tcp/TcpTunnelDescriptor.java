package com.tuuzed.tunnel.server.tcp;

import com.tuuzed.tunnel.server.internal.ServerTunnelSessions;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class TcpTunnelDescriptor {
    @Nullable
    private final String addr;
    private final int port;
    @NotNull
    private final ServerTunnelSessions tunnelSessions;
    @Nullable
    private ChannelFuture bindChannelFuture;

    public TcpTunnelDescriptor(@Nullable String addr, int port, @NotNull ServerTunnelSessions tunnelSessions) {
        this.addr = addr;
        this.port = port;
        this.tunnelSessions = tunnelSessions;
    }

    @NotNull
    public ServerTunnelSessions tunnelSessions() {
        return tunnelSessions;
    }

    @Nullable
    public String addr() {
        return addr;
    }

    public int port() {
        return port;
    }

    public void open(@NotNull ServerBootstrap serverBootstrap) throws Exception {
        if (addr != null) {
            bindChannelFuture = serverBootstrap.bind(addr, port);
        } else {
            bindChannelFuture = serverBootstrap.bind(port);
        }
    }

    public void close() {
        if (bindChannelFuture != null) {
            bindChannelFuture.channel().close();
        }
        tunnelSessions.destroy();
    }
}
