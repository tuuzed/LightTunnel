package com.tuuzed.tunnel.server;

import com.tuuzed.tunnel.common.handler.TunnelHeartbeatHandler;
import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import com.tuuzed.tunnel.common.protocol.TunnelMessageDecoder;
import com.tuuzed.tunnel.common.protocol.TunnelMessageEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TunnelServer {

    private static final Logger logger = LoggerFactory.getLogger(TunnelServer.class);

    @NotNull
    private final NioEventLoopGroup bossGroup;
    @NotNull
    private final NioEventLoopGroup workerGroup;

    @Nullable
    private String bindAddr;
    private int bindPort;

    public TunnelServer(int bindPort) {
        this(null, bindPort);
    }

    public TunnelServer(@Nullable String bindAddr, int bindPort) {
        this.bossGroup = new NioEventLoopGroup();
        this.workerGroup = new NioEventLoopGroup();
        this.bindAddr = bindAddr;
        this.bindPort = bindPort;
    }

    public void start() throws Exception {
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast(new TunnelMessageDecoder())
                                    .addLast(new TunnelMessageEncoder())
                                    .addLast(new TunnelHeartbeatHandler())
                                    .addLast(new TunnelServerChannelHandler())
                            ;
                        }
                    });

            ChannelFuture f;
            if (bindAddr == null) {
                f = bootstrap.bind(bindPort).sync();
                logger.info("Serving Tunnel on any address port {}", bindPort);
            } else {
                f = bootstrap.bind(bindAddr, bindPort).sync();
                logger.info("Serving Tunnel on {} port {}", bindAddr, bindPort);
            }
            f.channel().closeFuture().sync();
        } finally {
            stop();
        }
    }

    public void stop() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

}
