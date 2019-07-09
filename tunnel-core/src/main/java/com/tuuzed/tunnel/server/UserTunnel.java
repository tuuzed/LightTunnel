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

    public UserTunnel(int bindPort, @NotNull Channel serverChannel) {
        this(null, bindPort, serverChannel);
    }

    public UserTunnel(@Nullable String bindAddr, int bindPort, @NotNull Channel serverChannel) {
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


    public void open() {
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

    public void close() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
