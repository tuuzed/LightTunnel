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

import static com.tuuzed.tunnel.common.protocol.TunnelConstants.ATTR_MAPPING;

public class UserTunnel {
    private static final Logger logger = LoggerFactory.getLogger(UserTunnel.class);

    @NotNull
    private final NioEventLoopGroup bossGroup;
    @NotNull
    private final NioEventLoopGroup workerGroup;

    @Nullable
    private final String bindAddr;
    private final int bindPort;
    @NotNull
    private final Channel serverChannel;

    private UserTunnel(@Nullable String bindAddr, int bindPort, @NotNull Channel serverChannel) {
        this.bossGroup = new NioEventLoopGroup();
        this.workerGroup = new NioEventLoopGroup();
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
            logger.info("Open Tunnel: {}", serverChannel.attr(ATTR_MAPPING));
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
    }

    @Override
    public String toString() {
        return "UserTunnel(" + bindPort + ")";
    }

    @NotNull
    public static Manager getManager() {
        return ManagerInstanceHolder.instance;
    }

    private static class ManagerInstanceHolder {
        private static final Manager instance = new Manager();
    }

    public static class Manager {
        private static final Logger logger = LoggerFactory.getLogger(Manager.class);

        private final Map<Channel, UserTunnel> serverChannelUserTunnels = new ConcurrentHashMap<>();
        private final Map<Integer, UserTunnel> bindPortUserTunnels = new ConcurrentHashMap<>();

        private Manager() {
        }

        public boolean hasBandedPort(int bindPort) {
            return bindPortUserTunnels.containsKey(bindPort);
        }


        @Nullable
        public UserTunnel getUserTunnel(int bindPort) {
            return bindPortUserTunnels.get(bindPort);
        }

        @Nullable
        public UserTunnel getUserTunnel(@NotNull Channel serverChannel) {
            return serverChannelUserTunnels.get(serverChannel);
        }

        @NotNull
        public UserTunnel openUserTunnel(int bindPort, @NotNull Channel serverChannel) throws BindException {
            return openUserTunnel(null, bindPort, serverChannel);
        }

        @NotNull
        public UserTunnel openUserTunnel(@Nullable String bindAddr, int bindPort, @NotNull Channel serverChannel) throws BindException {
            if (hasBandedPort(bindPort)) {
                throw new BindException("bindPort: " + bindPort);
            }
            UserTunnel tunnel = new UserTunnel(bindAddr, bindPort, serverChannel);
            tunnel.open();
            bindPortUserTunnels.put(bindPort, tunnel);
            serverChannelUserTunnels.put(serverChannel, tunnel);
            return tunnel;
        }


        public void closeUserTunnel(@NotNull Channel serverChannel) {
            UserTunnel tunnel = serverChannelUserTunnels.remove(serverChannel);
            if (tunnel != null) {
                bindPortUserTunnels.remove(tunnel.bindPort());
                tunnel.close();
                logger.info("Close Tunnel: {}", tunnel);
            }
        }

    }

}
