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

public class TunnelServer {

    private static final Logger logger = LoggerFactory.getLogger(TunnelServer.class);

    @NotNull
    private final NioEventLoopGroup bossGroup;
    @NotNull
    private final NioEventLoopGroup workerGroup;

    public TunnelServer() {
        this.bossGroup = new NioEventLoopGroup();
        this.workerGroup = new NioEventLoopGroup();
    }

    public void start(@NotNull String bindAddr, int bindPort) throws Exception {
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

            ChannelFuture f = bootstrap.bind(bindAddr, bindPort).sync();
            logger.info("Serving Tunnel on {} port {}", bindAddr, bindPort);
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
