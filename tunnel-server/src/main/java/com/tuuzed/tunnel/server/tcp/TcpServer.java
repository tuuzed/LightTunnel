package com.tuuzed.tunnel.server.tcp;

import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import com.tuuzed.tunnel.server.internal.ServerTunnelSessions;
import com.tuuzed.tunnel.server.stats.Stats;
import com.tuuzed.tunnel.server.stats.StatsHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TcpServer {
    private static final Logger logger = LoggerFactory.getLogger(TcpServer.class);

    @NotNull
    private final Map<Long, Descriptor> tunnelTokenDescriptors = new ConcurrentHashMap<>();
    @NotNull
    private final Map<Integer, Descriptor> portDescriptors = new ConcurrentHashMap<>();
    @NotNull
    private final Object descriptorsLock = new Object();

    @NotNull
    private final ServerBootstrap serverBootstrap;


    public TcpServer(
        @NotNull final NioEventLoopGroup bossGroup,
        @NotNull final NioEventLoopGroup workerGroup,
        @NotNull final Stats stats
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
                        .addFirst(new StatsHandler(stats))
                        .addLast(new TcpServerChannelHandler(TcpServer.this))
                    ;
                }
            });
    }


    public void startTunnel(@Nullable String addr, int port, @NotNull ServerTunnelSessions tunnelSessions) throws Exception {
        final Descriptor descriptor = new Descriptor(addr, port, tunnelSessions);
        descriptor.open(serverBootstrap);
        synchronized (descriptorsLock) {
            tunnelTokenDescriptors.put(tunnelSessions.tunnelToken(), descriptor);
            portDescriptors.put(port, descriptor);
        }
        logger.info("Start Tunnel: {}", tunnelSessions.protoRequest());
    }


    public void shutdownTunnel(long tunnelToken) {
        synchronized (descriptorsLock) {
            final Descriptor descriptor = tunnelTokenDescriptors.remove(tunnelToken);
            if (descriptor != null) {
                portDescriptors.remove(descriptor.port);
                descriptor.close();
                logger.info("Shutdown Tunnel: {}", descriptor.tunnelSessions.protoRequest());
            }
        }
    }

    @Nullable
    public Descriptor getDescriptorTunnelToken(long tunnelToken) {
        synchronized (descriptorsLock) {
            return tunnelTokenDescriptors.get(tunnelToken);
        }
    }

    @Nullable
    public Descriptor getDescriptorByPort(int port) {
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

    public static class Descriptor {
        @Nullable
        private final String addr;
        private final int port;
        @NotNull
        private final ServerTunnelSessions tunnelSessions;
        @Nullable
        private ChannelFuture bindChannelFuture;

        @NotNull
        public ServerTunnelSessions tunnelSessions() {
            return tunnelSessions;
        }

        private Descriptor(@Nullable String addr, int port, @NotNull ServerTunnelSessions tunnelSessions) {
            this.addr = addr;
            this.port = port;
            this.tunnelSessions = tunnelSessions;
        }

        private void open(@NotNull ServerBootstrap serverBootstrap) throws Exception {
            if (addr != null) {
                bindChannelFuture = serverBootstrap.bind(addr, port);
            } else {
                bindChannelFuture = serverBootstrap.bind(port);
            }
        }

        private void close() {
            if (bindChannelFuture != null) {
                bindChannelFuture.channel().close();
            }
            tunnelSessions.destroy();
        }
    }

}
