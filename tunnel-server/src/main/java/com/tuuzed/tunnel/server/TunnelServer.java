package com.tuuzed.tunnel.server;

import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import com.tuuzed.tunnel.common.proto.ProtoHeartbeatHandler;
import com.tuuzed.tunnel.common.proto.ProtoMessageDecoder;
import com.tuuzed.tunnel.common.proto.ProtoMessageEncoder;
import com.tuuzed.tunnel.common.proto.ProtoRequestInterceptor;
import com.tuuzed.tunnel.server.http.HttpRequestInterceptor;
import com.tuuzed.tunnel.server.http.HttpServer;
import com.tuuzed.tunnel.server.internal.TokenProducer;
import com.tuuzed.tunnel.server.tcp.TcpStats;
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
    private final ProtoRequestInterceptor protoRequestInterceptor;
    @NotNull
    private final HttpRequestInterceptor httpRequestInterceptor;

    @NotNull
    private final TcpStats stats;
    @NotNull
    private final TcpServer tcpServer;
    @NotNull
    private final HttpServer httpServer;
    @NotNull
    private final TokenProducer tunnelTokenProducer;

    TunnelServer(@NotNull TunnelServerBuilder builder) {
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
        this.httpRequestInterceptor = builder.httpRequestInterceptor;
        this.stats = new TcpStats();
        this.tcpServer = new TcpServer(bossGroup, workerGroup, stats);
        this.httpServer = new HttpServer(bossGroup, workerGroup, httpRequestInterceptor);
        this.tunnelTokenProducer = new TokenProducer();
    }

    @NotNull
    public static TunnelServerBuilder builder() {
        return new TunnelServerBuilder();
    }

    @NotNull
    public TcpStats stats() {
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
        tcpServer.destroy();
        httpServer.destroy();
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

}
