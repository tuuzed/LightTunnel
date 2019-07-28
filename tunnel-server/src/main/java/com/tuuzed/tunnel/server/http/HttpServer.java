package com.tuuzed.tunnel.server.http;

import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import com.tuuzed.tunnel.server.internal.ServerTunnelSessions;
import com.tuuzed.tunnel.server.tcp.TcpServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HttpServer {
    private static final Logger logger = LoggerFactory.getLogger(TcpServer.class);
    @NotNull
    private final ServerBootstrap serverBootstrap;
    @NotNull
    private final Map<Long, Descriptor> tunnelTokenDescriptors = new ConcurrentHashMap<>();
    @NotNull
    private final Map<String, Descriptor> vhostDescriptors = new ConcurrentHashMap<>();

    @NotNull
    private final Object descriptorsLock = new Object();

    public HttpServer(
        @NotNull final NioEventLoopGroup bossGroup,
        @NotNull final NioEventLoopGroup workerGroup
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
                        .addLast(new HttpRequestDecoder())
                        .addLast(new HttpServerChannelHandler(HttpServer.this))
                    ;
                }
            });
    }


    public void register(@NotNull String vhost, @NotNull ServerTunnelSessions tunnelSessions) throws Exception {
        final Descriptor descriptor = new Descriptor(vhost, tunnelSessions);
        synchronized (descriptorsLock) {
            tunnelTokenDescriptors.put(tunnelSessions.tunnelToken(), descriptor);
            vhostDescriptors.put(vhost, descriptor);
        }
        logger.info("Start Tunnel: {}", tunnelSessions.protoRequest());
        logger.trace("vhostDescriptors: {}", vhostDescriptors);
        logger.trace("tunnelTokenDescriptors: {}", tunnelTokenDescriptors);

    }


    public void unregister(long tunnelToken) {
        synchronized (descriptorsLock) {
            final Descriptor descriptor = tunnelTokenDescriptors.remove(tunnelToken);
            if (descriptor != null) {
                vhostDescriptors.remove(descriptor.vhost);
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
    public Descriptor getDescriptorByVhost(@NotNull String vhost) {
        synchronized (descriptorsLock) {
            return vhostDescriptors.get(vhost);
        }
    }

    public void serve(@Nullable String bindAddr, int bindPort) throws Exception {
        if (bindAddr != null) {
            serverBootstrap.bind(bindAddr, bindPort).get();
        } else {
            serverBootstrap.bind(bindPort).get();
        }
    }

    public void destroy() {
        synchronized (descriptorsLock) {
            tunnelTokenDescriptors.clear();
            vhostDescriptors.clear();
        }
    }

    public static class Descriptor {
        @NotNull
        private final String vhost;
        @NotNull
        private final ServerTunnelSessions tunnelSessions;

        Descriptor(@NotNull String vhost, @NotNull ServerTunnelSessions tunnelSessions) {
            this.vhost = vhost;
            this.tunnelSessions = tunnelSessions;
        }

        @NotNull
        public String vhost() {
            return vhost;
        }

        @NotNull
        public ServerTunnelSessions tunnelSessions() {
            return tunnelSessions;
        }

        @Override
        public String toString() {
            return "Descriptor{" +
                "vhost='" + vhost + '\'' +
                ", tunnelSessions=" + tunnelSessions +
                '}';
        }
    }


}
