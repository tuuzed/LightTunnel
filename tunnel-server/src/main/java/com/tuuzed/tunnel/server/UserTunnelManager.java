package com.tuuzed.tunnel.server;

import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import com.tuuzed.tunnel.common.protocol.TunnelAttributeKey;
import com.tuuzed.tunnel.server.stat.StatHandler;
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


public class UserTunnelManager {
    private static final Logger logger = LoggerFactory.getLogger(UserTunnelManager.class);

    @NotNull
    public static UserTunnelManager getInstance() {
        return InstanceHolder.instance;
    }

    private static class InstanceHolder {
        private static final UserTunnelManager instance = new UserTunnelManager();
    }

    /**
     * 隧道ID生成器
     */
    private final AtomicLong tunnelTokenGenerator = new AtomicLong();

    private final Map<Integer, UserTunnelImpl> bindPortUserTunnels = new ConcurrentHashMap<>();
    private final Map<Long, UserTunnelImpl> tunnelTokenUserTunnels = new ConcurrentHashMap<>();
    private final Map<Long, AtomicLong> tunnelTokenSessionTokenGenerator = new ConcurrentHashMap<>();
    @NotNull
    private final NioEventLoopGroup bossGroup;
    @NotNull
    private final NioEventLoopGroup workerGroup;
    @NotNull
    private final ServerBootstrap bootstrap;


    private UserTunnelManager() {
        this.bossGroup = new NioEventLoopGroup();
        this.workerGroup = new NioEventLoopGroup();
        this.bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addFirst(new StatHandler())
                                .addLast(new UserTunnelChannelHandler())
                        ;
                    }
                });
    }

    public boolean hasBandedPort(int bindPort) {
        return bindPortUserTunnels.containsKey(bindPort);
    }

    @Nullable
    public UserTunnel getUserTunnelByTunnelToken(long tunnelToken) {
        return tunnelTokenUserTunnels.get(tunnelToken);
    }

    @Nullable
    public UserTunnel getUserTunnelByBindPort(int bindPort) {
        return bindPortUserTunnels.get(bindPort);
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
        UserTunnelImpl tunnel = new UserTunnelImpl(bindAddr, bindPort, serverChannel);
        tunnel.open(bootstrap);
        bindPortUserTunnels.put(bindPort, tunnel);
        long tunnelToken = tunnelTokenGenerator.incrementAndGet();
        tunnelTokenUserTunnels.put(tunnelToken, tunnel);
        return tunnelToken;
    }

    public void closeUserTunnel(long tunnelToken) {
        tunnelTokenSessionTokenGenerator.remove(tunnelToken);
        UserTunnelImpl tunnel = tunnelTokenUserTunnels.remove(tunnelToken);
        if (tunnel != null) {
            bindPortUserTunnels.remove(tunnel.bindPort());
            tunnel.close();
            logger.info("Close Tunnel: {}", tunnel);
        }
    }

    public int getUserTunnelTotalCount() {
        return bindPortUserTunnels.size();
    }

    private static class UserTunnelImpl implements UserTunnel {

        @Nullable
        private final String bindAddr;
        private final int bindPort;
        @NotNull
        private final Channel serverChannel;
        @NotNull
        private final Map<String, Channel> cachedUserTunnelChannels = new ConcurrentHashMap<>();
        @Nullable
        private ChannelFuture bindChannelFuture;

        private UserTunnelImpl(@Nullable String bindAddr, int bindPort, @NotNull Channel serverChannel) {
            this.bindAddr = bindAddr;
            this.bindPort = bindPort;
            this.serverChannel = serverChannel;
        }

        @NotNull
        @Override
        public Channel serverChannel() {
            return serverChannel;
        }

        @Override
        public int bindPort() {
            return bindPort;
        }

        @Override
        public void putUserTunnelChannel(long tunnelToken, long sessionToken, @NotNull Channel channel) {
            final String key = getCachedUserTunnelChannelKey(tunnelToken, sessionToken);
            cachedUserTunnelChannels.put(key, channel);
        }

        @Nullable
        @Override
        public Channel getUserTunnelChannel(long tunnelToken, long sessionToken) {
            final String key = getCachedUserTunnelChannelKey(tunnelToken, sessionToken);
            return cachedUserTunnelChannels.get(key);
        }

        @Override
        public void removeUserTunnelChannel(long tunnelToken, long sessionToken) {
            final String key = getCachedUserTunnelChannelKey(tunnelToken, sessionToken);
            Channel channel = cachedUserTunnelChannels.remove(key);
            if (channel != null) {
                channel.close();
            }
        }

        @Override
        public String toString() {
            return "UserTunnel:" + bindPort;
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
            return String.format("%d@%d", tunnelToken, sessionToken);
        }

    }


}
