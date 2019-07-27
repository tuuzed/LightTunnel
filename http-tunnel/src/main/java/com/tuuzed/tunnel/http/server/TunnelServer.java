package com.tuuzed.tunnel.http.server;

import com.tuuzed.tunnel.common.protocol.TunnelMessageDecoder;
import com.tuuzed.tunnel.common.protocol.TunnelMessageEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.jetbrains.annotations.NotNull;

public class TunnelServer {

    @NotNull
    private final NioEventLoopGroup bossGroup;
    @NotNull
    private final NioEventLoopGroup workerGroup;
    @NotNull
    private final TunnelServerChannels tunnelServerChannels;
    private final HttpServerChannels httpServerChannels;

    public TunnelServer(@NotNull NioEventLoopGroup bossGroup,
                        @NotNull NioEventLoopGroup workerGroup,
                        @NotNull TunnelServerChannels tunnelServerChannels,
                        @NotNull HttpServerChannels httpServerChannels
    ) {
        this.bossGroup = bossGroup;
        this.workerGroup = workerGroup;
        this.tunnelServerChannels = tunnelServerChannels;
        this.httpServerChannels = httpServerChannels;
    }

    public void start(final int port) throws Exception {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast(new TunnelMessageDecoder())
                                .addLast(new TunnelMessageEncoder())
                                .addLast(new TunnelServerChannelHandler(tunnelServerChannels, httpServerChannels))
                        ;
                    }
                });
        bootstrap.bind(port).get();
    }


}
