package com.tuuzed.tunnel.server;

import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import com.tuuzed.tunnel.common.protocol.TunnelAttributeKey;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.BindException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;


class UserTunnel {
    private static final Logger logger = LoggerFactory.getLogger(UserTunnel.class);
    /**
     * 隧道ID生成器
     */
    @NotNull
    private final AtomicLong tunnelTokenGenerator;
    @NotNull
    private final Map<Integer, Descriptor> bindPortDescriptors;
    @NotNull
    private final Map<Long, Descriptor> tunnelTokenDescriptors;
    @NotNull
    private final Map<Long, AtomicLong> tunnelTokenSessionTokenGenerator;
    @NotNull
    private final ServerBootstrap bootstrap;

    public UserTunnel(
            @NotNull final NioEventLoopGroup bossGroup,
            @NotNull final NioEventLoopGroup workerGroup,
            @NotNull final Stats stats
    ) {
        this.tunnelTokenGenerator = new AtomicLong();
        this.bindPortDescriptors = new ConcurrentHashMap<>();
        this.tunnelTokenDescriptors = new ConcurrentHashMap<>();
        this.tunnelTokenSessionTokenGenerator = new ConcurrentHashMap<>();
        this.bootstrap = new ServerBootstrap();
        this.bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addFirst(new StatsHandler(stats))
                                .addLast(new UserTunnelChannelHandler(UserTunnel.this))
                        ;
                    }
                });
    }

    public boolean hasBandedPort(int bindPort) {
        return bindPortDescriptors.containsKey(bindPort);
    }

    @Nullable
    public Descriptor getUserTunnelDescriptorByTunnelToken(long tunnelToken) {
        return tunnelTokenDescriptors.get(tunnelToken);
    }

    @Nullable
    public Descriptor getUserTunnelDescriptorByBindPort(int bindPort) {
        return bindPortDescriptors.get(bindPort);
    }

    public long generateSessionToken(long tunnelToken) {
        AtomicLong sessionTokenGenerator = tunnelTokenSessionTokenGenerator.get(tunnelToken);
        if (sessionTokenGenerator == null) {
            sessionTokenGenerator = new AtomicLong();
            tunnelTokenSessionTokenGenerator.put(tunnelToken, sessionTokenGenerator);
        }
        return sessionTokenGenerator.incrementAndGet();
    }

    public long openUserTunnel(int bindPort, @NotNull Channel serverChannel) throws BindException {
        return openUserTunnel(null, bindPort, serverChannel);
    }

    public long openUserTunnel(@Nullable String bindAddr, int bindPort, @NotNull Channel serverChannel) throws BindException {
        if (hasBandedPort(bindPort)) {
            throw new BindException("bindPort: " + bindPort);
        }
        Descriptor descriptor = new Descriptor(bindAddr, bindPort, serverChannel);
        descriptor.open(bootstrap);
        bindPortDescriptors.put(bindPort, descriptor);
        long tunnelToken = tunnelTokenGenerator.incrementAndGet();
        tunnelTokenDescriptors.put(tunnelToken, descriptor);
        return tunnelToken;
    }

    public void closeUserTunnel(long tunnelToken) {
        tunnelTokenSessionTokenGenerator.remove(tunnelToken);
        Descriptor tunnel = tunnelTokenDescriptors.remove(tunnelToken);
        if (tunnel != null) {
            bindPortDescriptors.remove(tunnel.bindPort());
            tunnel.close();
            logger.info("Close Tunnel: {}", tunnel);
        }
    }

    public void destroy() {
        bindPortDescriptors.clear();
        tunnelTokenDescriptors.clear();
        tunnelTokenSessionTokenGenerator.clear();
    }

    public static class Descriptor {

        @Nullable
        private final String bindAddr;
        private final int bindPort;
        @NotNull
        private final Channel serverChannel;
        @NotNull
        private final Map<String, Channel> cachedUserTunnelChannels = new ConcurrentHashMap<>();
        @Nullable
        private ChannelFuture bindChannelFuture;

        private Descriptor(@Nullable String bindAddr, int bindPort, @NotNull Channel serverChannel) {
            this.bindAddr = bindAddr;
            this.bindPort = bindPort;
            this.serverChannel = serverChannel;
        }

        @NotNull
        public Channel serverChannel() {
            return serverChannel;
        }

        public int bindPort() {
            return bindPort;
        }

        public void putUserTunnelChannel(long tunnelToken, long sessionToken, @NotNull Channel channel) {
            final String key = getCachedUserTunnelChannelKey(tunnelToken, sessionToken);
            cachedUserTunnelChannels.put(key, channel);
        }

        @Nullable
        public Channel getUserTunnelChannel(long tunnelToken, long sessionToken) {
            final String key = getCachedUserTunnelChannelKey(tunnelToken, sessionToken);
            return cachedUserTunnelChannels.get(key);
        }

        public void removeUserTunnelChannel(long tunnelToken, long sessionToken) {
            final String key = getCachedUserTunnelChannelKey(tunnelToken, sessionToken);
            Channel channel = cachedUserTunnelChannels.remove(key);
            if (channel != null) {
                channel.close();
            }
        }


        private void open(@NotNull ServerBootstrap bootstrap) {
            try {
                if (bindAddr == null) {
                    bindChannelFuture = bootstrap.bind(bindPort);
                } else {
                    bindChannelFuture = bootstrap.bind(bindAddr, bindPort);
                }
                logger.info("Open Tunnel: {}", serverChannel.attr(TunnelAttributeKey.OPEN_TUNNEL_REQUEST).get());
            } catch (Exception ex) {
                // BindException表示该端口已经绑定过
                if (!(ex.getCause() instanceof BindException)) {
                    throw new RuntimeException(ex);
                }
            }
        }

        private void close() {
            if (bindChannelFuture != null) {
                bindChannelFuture.channel().close();
            }
            cachedUserTunnelChannels.clear();
        }

        @NotNull
        private String getCachedUserTunnelChannelKey(long tunnelToken, long sessionToken) {
            return String.format("%d-%d", tunnelToken, sessionToken);
        }

        @Override
        public String toString() {
            return "UserTunnel:" + bindPort;
        }


    }


}
