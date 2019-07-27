package com.tuuzed.tunnel.http.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import org.jetbrains.annotations.NotNull;

public class HttpServer {

    @NotNull
    private final NioEventLoopGroup bossGroup;
    @NotNull
    private final NioEventLoopGroup workerGroup;
    @NotNull
    private final TunnelServerChannels tunnelServerChannels;
    @NotNull
    private final HttpServerChannels httpServerChannels;

    public HttpServer(@NotNull NioEventLoopGroup bossGroup,
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
                                .addLast(new HttpRequestDecoder())
                                .addLast(new HttpServerChannelHandler(tunnelServerChannels, httpServerChannels))
                        ;
                    }
                });
        bootstrap.bind(port).get();
    }
}
