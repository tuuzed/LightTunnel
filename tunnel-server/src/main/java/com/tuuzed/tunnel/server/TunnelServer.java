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
    @NotNull
    private final ServerBootstrap bootstrap;
    @Nullable
    private final String bindAddr;
    private final int bindPort;
    @NotNull
    private final UserTunnelManager userTunnelManager;
    // ssl
    @Nullable
    private ServerBootstrap sslBootstrap;
    private final int sslBindPort;
    // 数据统计
    @NotNull
    private final Statisticians statisticians;

    private TunnelServer(@NotNull final Builder builder) {
        this.bootstrap = new ServerBootstrap();
        this.bossGroup = (builder.bossThreads > 0)
                ? new NioEventLoopGroup(builder.bossThreads)
                : new NioEventLoopGroup();
        this.workerGroup = (builder.workerThreads > 0)
                ? new NioEventLoopGroup(builder.workerThreads)
                : new NioEventLoopGroup();
        this.statisticians = new Statisticians();

        this.userTunnelManager = new UserTunnelManager(this.bossGroup, this.workerGroup, statisticians);
        this.bindAddr = builder.bindAddr;
        this.bindPort = builder.bindPort;
        this.bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast(new TunnelMessageDecoder())
                                .addLast(new TunnelMessageEncoder())
                                .addLast(new TunnelHeartbeatHandler())
                                .addLast(new TunnelServerChannelHandler(userTunnelManager, builder.interceptor))
                        ;
                    }
                });

        // ssl
        this.sslBindPort = builder.sslBindPort;
        if (builder.ssl) {
            this.sslBootstrap = new ServerBootstrap();
            this.sslBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast(new SslHandler(builder.context.newEngine(ch.alloc())))
                                    .addLast(new TunnelMessageDecoder())
                                    .addLast(new TunnelMessageEncoder())
                                    .addLast(new TunnelHeartbeatHandler())
                                    .addLast(new TunnelServerChannelHandler(userTunnelManager, builder.interceptor))
                            ;
                        }
                    });
        }


    }

    @NotNull
    public Statisticians getStatisticians() {
        return statisticians;
    }

    public void start() throws Exception {
        if (bindAddr == null) {
            bootstrap.bind(bindPort).get();
            logger.info("Serving Tunnel on any address port {}", bindPort);
            if (sslBootstrap != null) {
                sslBootstrap.bind(sslBindPort).get();
                logger.info("Serving SSL Tunnel on any address port {}", sslBindPort);
            }
        } else {
            bootstrap.bind(bindAddr, bindPort).sync();
            logger.info("Serving Tunnel on {} port {}", bindAddr, bindPort);
            if (sslBootstrap != null) {
                sslBootstrap.bind(bindAddr, sslBindPort).sync();
                logger.info("Serving SSL Tunnel on {} port {}", bindAddr, sslBindPort);
            }
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
