package com.tuuzed.tunnel.server;

import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import com.tuuzed.tunnel.common.proto.ProtoHeartbeatHandler;
import com.tuuzed.tunnel.common.proto.ProtoMessageDecoder;
import com.tuuzed.tunnel.common.proto.ProtoMessageEncoder;
import com.tuuzed.tunnel.common.proto.ProtoRequest;
import com.tuuzed.tunnel.server.http.HttpServer;
import com.tuuzed.tunnel.server.internal.TokenProducer;
import com.tuuzed.tunnel.server.stats.Stats;
import com.tuuzed.tunnel.server.tcp.TcpServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
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
    // ssl
    private final boolean enableSsl;
    @Nullable
    private final SslContext sslContext;
    @Nullable
    private final String sslBindAddr;
    private final int sslBindPort;
    // http
    private final String httpBindAddr;
    private final int httpBindPort;
    //
    @NotNull
    private final ProtoRequest.Interceptor protoRequestInterceptor;
    @NotNull
    private final Stats stats;
    @NotNull
    private final TcpServer tcpServer;
    @NotNull
    private final HttpServer httpServer;
    @NotNull
    private final TokenProducer tunnelTokenProducer;

    private TunnelServer(@NotNull Builder builder) {
        this.bossGroup = (builder.bossThreads > 0)
            ? new NioEventLoopGroup(builder.bossThreads)
            : new NioEventLoopGroup();
        this.workerGroup = (builder.workerThreads > 0)
            ? new NioEventLoopGroup(builder.workerThreads)
            : new NioEventLoopGroup();

        this.bindAddr = builder.bindAddr;
        this.bindPort = builder.bindPort;

        this.enableSsl = builder.enableSsl;
        this.sslContext = builder.sslContext;
        this.sslBindAddr = builder.sslBindAddr;
        this.sslBindPort = builder.sslBindPort;

        this.httpBindAddr = builder.httpBindAddr;
        this.httpBindPort = builder.httpBindPort;

        this.protoRequestInterceptor = builder.protoRequestInterceptor;
        this.stats = new Stats();
        this.tcpServer = new TcpServer(bossGroup, workerGroup, stats);
        this.httpServer = new HttpServer(bossGroup, workerGroup);
        this.tunnelTokenProducer = new TokenProducer();

    }

    @NotNull
    public Stats stats() {
        return stats;
    }


    public void start() throws Exception {
        serve();
        if (enableSsl) {
            serveWithSsl();
        }
        httpServe();
    }

    private void httpServe() throws Exception {
        httpServer.serve(httpBindAddr, httpBindPort);
        if (httpBindAddr == null) {
            logger.info("Serving Http on any address port {}", httpBindPort);
        } else {
            logger.info("Serving Http on {} port {}", httpBindAddr, httpBindPort);
        }
    }

    private void serve() throws Exception {
        final ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .childOption(ChannelOption.AUTO_READ, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline()
                        .addLast(new ProtoMessageDecoder())
                        .addLast(new ProtoMessageEncoder())
                        .addLast(new ProtoHeartbeatHandler())
                        .addLast(new TunnelServerChannelHandler(
                            tcpServer, httpServer, protoRequestInterceptor, tunnelTokenProducer
                        ))
                    ;
                }
            });
        if (bindAddr == null) {
            serverBootstrap.bind(bindPort).get();
            logger.info("Serving Tunnel on any address port {}", bindPort);
        } else {
            serverBootstrap.bind(bindAddr, bindPort).get();
            logger.info("Serving Tunnel on {} port {}", bindAddr, bindPort);
        }
    }

    private void serveWithSsl() throws Exception {
        if (sslContext == null) {
            return;
        }
        final ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline()
                        .addLast(new SslHandler(sslContext.newEngine(ch.alloc())))
                        .addLast(new ProtoMessageDecoder())
                        .addLast(new ProtoMessageEncoder())
                        .addLast(new ProtoHeartbeatHandler())
                        .addLast(new TunnelServerChannelHandler(
                            tcpServer, httpServer, protoRequestInterceptor, tunnelTokenProducer
                        ))
                    ;
                }
            });
        if (sslBindAddr == null) {
            serverBootstrap.bind(sslBindPort).get();
            logger.info("Serving SSL Tunnel on any address port {}", sslBindPort);
        } else {
            serverBootstrap.bind(sslBindAddr, sslBindPort).get();
            logger.info("Serving SSL Tunnel on {} port {}", sslBindAddr, sslBindPort);
        }
    }

    public void destroy() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        tcpServer.destroy();
        httpServer.destroy();
    }

    public static class Builder {
        private int bossThreads = -1;
        private int workerThreads = -1;

        private String bindAddr = null;
        private int bindPort = 5000;

        // ssl
        private boolean enableSsl;
        private SslContext sslContext;
        private String sslBindAddr = null;
        private int sslBindPort = 5001;
        // http
        private String httpBindAddr = null;
        private int httpBindPort = 5080;

        //
        private ProtoRequest.Interceptor protoRequestInterceptor = ProtoRequest.Interceptor.DEFAULT;


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
        public Builder enableSsl(@NotNull SslContext sslContext, int bindPort) {
            this.enableSsl = true;
            this.sslContext = sslContext;
            this.sslBindPort = bindPort;
            return this;
        }

        @NotNull
        public Builder enableSsl(@NotNull SslContext sslContext, String bindAddr, int bindPort) {
            this.enableSsl = true;
            this.sslContext = sslContext;
            this.sslBindAddr = bindAddr;
            this.sslBindPort = bindPort;
            return this;
        }

        @NotNull
        public Builder setHttpBindAddr(String bindAddr) {
            this.httpBindAddr = bindAddr;
            return this;
        }

        @NotNull
        public Builder setHttpBindPort(int bindPort) {
            this.httpBindPort = bindPort;
            return this;
        }

        @NotNull
        public Builder setProtoRequestInterceptor(@NotNull ProtoRequest.Interceptor interceptor) {
            this.protoRequestInterceptor = interceptor;
            return this;
        }

        @NotNull
        public TunnelServer build() {
            return new TunnelServer(this);
        }
    }


}
