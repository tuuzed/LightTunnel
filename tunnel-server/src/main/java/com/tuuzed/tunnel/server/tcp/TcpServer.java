package com.tuuzed.tunnel.server.tcp;

import com.tuuzed.tunnel.server.internal.ServerTunnelSessions;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TcpServer {
    private static final Logger logger = LoggerFactory.getLogger(TcpServer.class);

    @NotNull
    private final Map<Long, TcpTunnelDescriptor> tunnelTokenDescriptors = new ConcurrentHashMap<>();
    @NotNull
    private final Map<Integer, TcpTunnelDescriptor> portDescriptors = new ConcurrentHashMap<>();
    @NotNull
    private final Object descriptorsLock = new Object();
    @NotNull
    private final ServerBootstrap serverBootstrap;


    public TcpServer(
        @NotNull final NioEventLoopGroup bossGroup,
        @NotNull final NioEventLoopGroup workerGroup,
        @NotNull final TcpStats stats
    ) {
        this.serverBootstrap = new ServerBootstrap();
        this.serverBootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .childOption(ChannelOption.AUTO_READ, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline()
                        .addFirst(new TcpStatsHandler(stats))
                        .addLast(new TcpServerChannelHandler(TcpServer.this))
                    ;
                }
            });
    }


    public void startTunnel(
        @Nullable String addr, int port,
        @NotNull ServerTunnelSessions tunnelSessions
    ) throws Exception {
        final TcpTunnelDescriptor descriptor = new TcpTunnelDescriptor(addr, port, tunnelSessions);
        descriptor.open(serverBootstrap);
        synchronized (descriptorsLock) {
            tunnelTokenDescriptors.put(tunnelSessions.tunnelToken(), descriptor);
            portDescriptors.put(port, descriptor);
        }
        logger.info("Start Tunnel: {}", tunnelSessions.protoRequest());
    }


    public void shutdownTunnel(long tunnelToken) {
        synchronized (descriptorsLock) {
            final TcpTunnelDescriptor descriptor = tunnelTokenDescriptors.remove(tunnelToken);
            if (descriptor != null) {
                portDescriptors.remove(descriptor.port());
                descriptor.close();
                logger.info("Shutdown Tunnel: {}", descriptor.tunnelSessions().protoRequest());
            }
        }
    }

    @Nullable
    public Channel getSessionChannel(long tunnelToken, long sessionToken) {
        TcpTunnelDescriptor descriptor;
        synchronized (descriptorsLock) {
            descriptor = tunnelTokenDescriptors.get(tunnelToken);
        }
        if (descriptor == null) {
            return null;
        }
        return descriptor.tunnelSessions().getSessionChannel(sessionToken);
    }


    @Nullable
    TcpTunnelDescriptor getDescriptorByPort(int port) {
        synchronized (descriptorsLock) {
            return portDescriptors.get(port);
        }
    }

    public void destroy() {
        synchronized (descriptorsLock) {
            tunnelTokenDescriptors.clear();
            portDescriptors.clear();
        }
    }


}
