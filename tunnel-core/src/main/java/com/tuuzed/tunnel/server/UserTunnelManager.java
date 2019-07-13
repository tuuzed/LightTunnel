package com.tuuzed.tunnel.server;

import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
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

import static com.tuuzed.tunnel.common.protocol.TunnelConstants.ATTR_OPEN_TUNNEL_REQUEST;

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

    private UserTunnelManager() {
    }

    /**
     * 判断是否已经绑定端口
     */
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


    public long openUserTunnel(int bindPort, @NotNull Channel serverChannel) throws BindException {
        return openUserTunnel(null, bindPort, serverChannel);
    }

    public long openUserTunnel(@Nullable String bindAddr, int bindPort, @NotNull Channel serverChannel) throws BindException {
        if (hasBandedPort(bindPort)) {
            throw new BindException("bindPort: " + bindPort);
        }
        UserTunnelImpl tunnel = new UserTunnelImpl(bindAddr, bindPort, serverChannel);
        tunnel.open();
        bindPortUserTunnels.put(bindPort, tunnel);
        long tunnelToken = tunnelTokenGenerator.incrementAndGet();
        tunnelTokenUserTunnels.put(tunnelToken, tunnel);
        return tunnelToken;
    }


    /**
     * 关闭隧道
     *
     * @param tunnelToken 隧道令牌
     */
    public void closeUserTunnel(long tunnelToken) {
        UserTunnelImpl tunnel = tunnelTokenUserTunnels.remove(tunnelToken);
        if (tunnel != null) {
            bindPortUserTunnels.remove(tunnel.bindPort());
            tunnel.close();
            logger.info("Close Tunnel: {}", tunnel);
        }
    }


    private static class UserTunnelImpl implements UserTunnel {
        @NotNull
        private final NioEventLoopGroup bossGroup;
        @NotNull
        private final NioEventLoopGroup workerGroup;
        @Nullable
        private final String bindAddr;
        private final int bindPort;
        @NotNull
        private final Channel serverChannel;
        @NotNull
        private final Map<String, Channel> tunnelTokenSessionTokenUserTunnelChannels = new ConcurrentHashMap<>();
        @NotNull
        private final AtomicLong sessionTokenGenerator;

        private UserTunnelImpl(@Nullable String bindAddr, int bindPort, @NotNull Channel serverChannel) {
            this.bossGroup = new NioEventLoopGroup();
            this.workerGroup = new NioEventLoopGroup();
            this.bindAddr = bindAddr;
            this.bindPort = bindPort;
            this.serverChannel = serverChannel;
            this.sessionTokenGenerator = new AtomicLong();
        }

        private void open() {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new UserTunnelChannelHandler());
                        }
                    });
            try {
                if (bindAddr == null) {
                    bootstrap.bind(bindPort).get();
                } else {
                    bootstrap.bind(bindAddr, bindPort).get();
                }
                logger.info("Open Tunnel: {}", serverChannel.attr(ATTR_OPEN_TUNNEL_REQUEST).get());
            } catch (Exception ex) {
                // BindException表示该端口已经绑定过
                if (!(ex.getCause() instanceof BindException)) {
                    throw new RuntimeException(ex);
                }
            }
        }

        private void close() {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            tunnelTokenSessionTokenUserTunnelChannels.clear();
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
        public long generateSessionToken() {
            return sessionTokenGenerator.incrementAndGet();
        }

        @Override
        public void putUserTunnelChannel(long tunnelToken, long sessionToken, @NotNull Channel channel) {
            final String key = getKey(tunnelToken, sessionToken);
            tunnelTokenSessionTokenUserTunnelChannels.put(key, channel);
        }

        @Nullable
        @Override
        public Channel getUserTunnelChannel(long tunnelToken, long sessionToken) {
            final String key = getKey(tunnelToken, sessionToken);
            return tunnelTokenSessionTokenUserTunnelChannels.get(key);
        }

        @Override
        public void removeUserTunnelChannel(long tunnelToken, long sessionToken) {
            final String key = getKey(tunnelToken, sessionToken);
            Channel channel = tunnelTokenSessionTokenUserTunnelChannels.remove(key);
            channel.close();
        }

        @NotNull
        private static String getKey(long tunnelToken, long sessionToken) {
            return String.format("%d@%d", tunnelToken, sessionToken);
        }

        @Override
        public String toString() {
            return "UserTunnelImpl(" + bindPort + ")";
        }
    }


}
