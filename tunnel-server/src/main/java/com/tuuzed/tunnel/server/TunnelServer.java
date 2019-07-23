package com.tuuzed.tunnel.server;

import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import com.tuuzed.tunnel.common.protocol.OpenTunnelRequestInterceptor;
import com.tuuzed.tunnel.common.protocol.TunnelHeartbeatHandler;
import com.tuuzed.tunnel.common.protocol.TunnelMessageDecoder;
import com.tuuzed.tunnel.common.protocol.TunnelMessageEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TunnelServer {
    private static final Logger logger = LoggerFactory.getLogger(TunnelServer.class);

    @NotNull
    private final NioEventLoopGroup bossGroup;
    @NotNull
    private final NioEventLoopGroup workerGroup;
    @Nullable
    private final String bindAddr;
    private final int bindPort;
    private final int sslBindPort;
    @NotNull
    private final UserTunnel userTunnelManager;
    @NotNull
    private final OpenTunnelRequestInterceptor openTunnelRequestInterceptor;
    @Nullable
    private final SslContext sslContext;
    // 数据统计
    @NotNull
    private final Stats stats;

    private TunnelServer(@NotNull final Builder builder) {
        this.bossGroup = (builder.bossThreads > 0)
                ? new NioEventLoopGroup(builder.bossThreads)
                : new NioEventLoopGroup();
        this.workerGroup = (builder.workerThreads > 0)
                ? new NioEventLoopGroup(builder.workerThreads)
                : new NioEventLoopGroup();
        this.stats = new Stats();

        this.userTunnelManager = new UserTunnel(this.bossGroup, this.workerGroup, stats);
        this.openTunnelRequestInterceptor = builder.interceptor;
        this.bindAddr = builder.bindAddr;
        this.bindPort = builder.bindPort;
        this.sslBindPort = builder.sslBindPort;
        if (builder.ssl) {
            this.sslContext = builder.context;
        } else {
            this.sslContext = null;
        }
    }

    @NotNull
    public Stats getStats() {
        return stats;
    }

    public void start() throws Exception {
        serve();
        serveWithSsl();
    }


    private void serve() throws Exception {
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
                                .addLast(new TunnelServerChannelHandler(userTunnelManager, openTunnelRequestInterceptor))
                        ;
                    }
                });
        if (bindAddr == null) {
            bootstrap.bind(bindPort).get();
            logger.info("Serving Tunnel on any address port {}", bindPort);
        } else {
            bootstrap.bind(bindAddr, bindPort).sync();
            logger.info("Serving Tunnel on {} port {}", bindAddr, bindPort);
        }
    }

    private void serveWithSsl() throws Exception {
        if (sslContext == null) {
            return;
        }
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast(new SslHandler(sslContext.newEngine(ch.alloc())))
                                .addLast(new TunnelMessageDecoder())
                                .addLast(new TunnelMessageEncoder())
                                .addLast(new TunnelHeartbeatHandler())
                                .addLast(new TunnelServerChannelHandler(userTunnelManager, openTunnelRequestInterceptor))
                        ;
                    }
                });
        if (bindAddr == null) {
            bootstrap.bind(sslBindPort).get();
            logger.info("Serving SSL Tunnel on any address port {}", sslBindPort);
        } else {
            bootstrap.bind(bindAddr, sslBindPort).sync();
            logger.info("Serving SSL Tunnel on {} port {}", bindAddr, sslBindPort);
        }
    }

    public void destroy() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        userTunnelManager.destroy();
    }

    public static class Builder {
        private String bindAddr = null;
        private int bindPort = 5000;
        private OpenTunnelRequestInterceptor interceptor;
        private int bossThreads = -1;
        private int workerThreads = -1;

        // ssl
        private boolean ssl;
        private SslContext context;
        private int sslBindPort = 5001;

        @NotNull
        public Builder setBindAddr(String bindAddr) {
            this.bindAddr = bindAddr;
            return this;
        }

        @NotNull
        public Builder setBindPort(int bindPort) {
            this.bindPort = bindPort;
            return this;
        }

        @NotNull
        public Builder setBossThreads(int bossThreads) {
            this.bossThreads = bossThreads;
            return this;
        }

        @NotNull
        public Builder setWorkerThreads(int workerThreads) {
            this.workerThreads = workerThreads;
            return this;
        }

        @NotNull
        public Builder setInterceptor(@Nullable OpenTunnelRequestInterceptor interceptor) {
            this.interceptor = interceptor;
            return this;
        }

        @NotNull
        public Builder enableSsl(@NotNull SslContext context, int bindPort) {
            this.ssl = true;
            this.context = context;
            this.sslBindPort = bindPort;
            return this;
        }

        @NotNull
        public TunnelServer build() {
            return new TunnelServer(this);
        }
    }

}
