package com.tuuzed.tunnel.server;

import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import com.tuuzed.tunnel.common.proto.ProtoHeartbeatHandler;
import com.tuuzed.tunnel.common.proto.ProtoMessageDecoder;
import com.tuuzed.tunnel.common.proto.ProtoMessageEncoder;
import com.tuuzed.tunnel.common.interceptor.ProtoRequestInterceptor;
import com.tuuzed.tunnel.common.interceptor.HttpRequestInterceptor;
import com.tuuzed.tunnel.server.http.HttpServer;
import com.tuuzed.tunnel.server.internal.TokenProducer;
import com.tuuzed.tunnel.server.tcp.TcpServer;
import com.tuuzed.tunnel.server.tcp.TcpStats;
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

    @NotNull
    private final ProtoRequestInterceptor protoRequestInterceptor;
    //
    @Nullable
    private final String bindAddr;
    private final int bindPort;
    // ssl
    private final boolean sslEnable;
    @Nullable
    private final SslContext sslContext;
    @Nullable
    private final String sslBindAddr;
    private final int sslBindPort;

    // http
    private final boolean httpEnable;
    private final String httpBindAddr;
    private final int httpBindPort;
    @NotNull
    private final HttpRequestInterceptor httpRequestInterceptor;

    // https
    private final boolean httpsEnable;
    @Nullable
    private final SslContext httpsContext;
    @Nullable
    private final String httpsBindAddr;
    private final int httpsBindPort;
    @NotNull
    private final HttpRequestInterceptor httpsRequestInterceptor;

    //
    @NotNull
    private final TcpStats tcpStats;
    @NotNull
    private final TcpServer tcpServer;
    @Nullable
    private HttpServer httpServer = null;
    @Nullable
    private HttpServer httpsServer = null;
    @NotNull
    private final TokenProducer tunnelTokenProducer;

    TunnelServer(@NotNull TunnelServerBuilder builder) {
        this.bossGroup = (builder.bossThreads > 0)
            ? new NioEventLoopGroup(builder.bossThreads)
            : new NioEventLoopGroup();
        this.workerGroup = (builder.workerThreads > 0)
            ? new NioEventLoopGroup(builder.workerThreads)
            : new NioEventLoopGroup();

        this.protoRequestInterceptor = builder.protoRequestInterceptor;
        // auth
        this.bindAddr = builder.bindAddr;
        this.bindPort = builder.bindPort;
        // ssl auth
        this.sslEnable = builder.sslEnable;
        this.sslContext = builder.sslContext;
        this.sslBindAddr = builder.sslBindAddr;
        this.sslBindPort = builder.sslBindPort;

        // http
        this.httpEnable = builder.httpEnable;
        this.httpBindAddr = builder.httpBindAddr;
        this.httpBindPort = builder.httpBindPort;
        this.httpRequestInterceptor = builder.httpRequestInterceptor;
        // https
        this.httpsEnable = builder.httpsEnable;
        this.httpsContext = builder.httpsContext;
        this.httpsBindAddr = builder.httpsBindAddr;
        this.httpsBindPort = builder.httpsBindPort;
        this.httpsRequestInterceptor = builder.httpsRequestInterceptor;

        this.tcpStats = new TcpStats();
        this.tcpServer = new TcpServer(bossGroup, workerGroup, tcpStats);
        if (httpEnable) {
            this.httpServer = new HttpServer(bossGroup, workerGroup, httpRequestInterceptor, null);
        }
        if (httpsEnable) {
            this.httpsServer = new HttpServer(bossGroup, workerGroup, httpsRequestInterceptor, httpsContext);
        }
        this.tunnelTokenProducer = new TokenProducer();
    }

    @NotNull
    public static TunnelServerBuilder builder() {
        return new TunnelServerBuilder();
    }

    @NotNull
    public TcpStats tcpStats() {
        return tcpStats;
    }

    public void start() throws Exception {
        serve();
        if (sslEnable) {
            serveWithSsl();
        }
        httpServe();
        httpsServe();
    }

    private void httpServe() throws Exception {
        if (httpServer == null) {
            return;
        }
        httpServer.serve(httpBindAddr, httpBindPort);
        if (httpBindAddr == null) {
            logger.info("Serving Http on any address port {}", httpBindPort);
        } else {
            logger.info("Serving Http on {} port {}", httpBindAddr, httpBindPort);
        }
    }

    private void httpsServe() throws Exception {
        if (httpsServer == null) {
            return;
        }
        httpsServer.serve(httpsBindAddr, httpsBindPort);
        if (httpsBindAddr == null) {
            logger.info("Serving Https on any address port {}", httpsBindPort);
        } else {
            logger.info("Serving Https on {} port {}", httpsBindAddr, httpsBindPort);
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
                            tcpServer, httpServer, httpsServer, protoRequestInterceptor, tunnelTokenProducer
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
                            tcpServer, httpServer, httpsServer, protoRequestInterceptor, tunnelTokenProducer
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
        if (httpServer != null) {
            httpServer.destroy();
        }
        if (httpsServer != null) {
            httpsServer.destroy();
        }
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

}
