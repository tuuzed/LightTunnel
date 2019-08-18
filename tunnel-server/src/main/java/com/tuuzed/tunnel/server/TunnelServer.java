package com.tuuzed.tunnel.server;

import com.tuuzed.tunnel.common.interceptor.HttpRequestInterceptor;
import com.tuuzed.tunnel.common.interceptor.ProtoRequestInterceptor;
import com.tuuzed.tunnel.common.proto.ProtoHeartbeatHandler;
import com.tuuzed.tunnel.common.proto.ProtoMessageDecoder;
import com.tuuzed.tunnel.common.proto.ProtoMessageEncoder;
import com.tuuzed.tunnel.server.http.HttpServer;
import com.tuuzed.tunnel.server.http.HttpTunnelRegistry;
import com.tuuzed.tunnel.server.internal.TokenProducer;
import com.tuuzed.tunnel.server.tcp.TcpServer;
import com.tuuzed.tunnel.server.tcp.TcpTunnelStats;
import com.tuuzed.tunnel.server.tcp.TcpTunnelRegistry;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    @NotNull
    private final TcpServer tcpServer;
    @Nullable
    private HttpServer httpServer = null;
    @Nullable
    private HttpServer httpsServer = null;
    @NotNull
    private final TokenProducer tunnelTokenProducer;

    private TunnelServer(@NotNull Builder builder) {
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

        this.tcpServer = new TcpServer(bossGroup, workerGroup);
        if (httpEnable) {
            this.httpServer = new HttpServer(bossGroup, workerGroup, null, httpRequestInterceptor);
        }
        if (httpsEnable) {
            this.httpsServer = new HttpServer(bossGroup, workerGroup, httpsContext, httpsRequestInterceptor);
        }
        this.tunnelTokenProducer = new TokenProducer();
    }

    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    @NotNull
    public TcpTunnelStats tcpTunnelStats() {
        return tcpServer.stats();
    }

    @NotNull
    public TcpTunnelRegistry tcpTunnelRegistry() {
        return tcpServer.registry();
    }

    @Nullable
    public HttpTunnelRegistry httpTunnelRegistry() {
        if (httpServer != null) {
            return httpServer.registry();
        }
        return null;
    }

    @Nullable
    public HttpTunnelRegistry httpsTunnelRegistry() {
        if (httpsServer != null) {
            return httpsServer.registry();
        }
        return null;
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

    public static class Builder {

        // common
        /* package */ int bossThreads = -1;
        /* package */ int workerThreads = -1;

        // auth
        /* package */ String bindAddr = null;
        /* package */ int bindPort = 5000;
        /* package */ ProtoRequestInterceptor protoRequestInterceptor = ProtoRequestInterceptor.DEFAULT;

        // ssl auth
        /* package */ boolean sslEnable;
        /* package */ SslContext sslContext;
        /* package */ String sslBindAddr = null;
        /* package */ int sslBindPort = 5001;

        // http
        /* package */ boolean httpEnable;
        /* package */ String httpBindAddr = null;
        /* package */ int httpBindPort = 5080;
        /* package */ HttpRequestInterceptor httpRequestInterceptor = HttpRequestInterceptor.DEFAULT;

        // https
        /* package */ boolean httpsEnable;
        /* package */ SslContext httpsContext;
        /* package */ String httpsBindAddr = null;
        /* package */ int httpsBindPort = 5443;
        /* package */ HttpRequestInterceptor httpsRequestInterceptor = HttpRequestInterceptor.DEFAULT;


        /* package */ Builder() {
        }

        // ============================= common =============================== //
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

        // ============================= tcp auth =============================== //
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
        public Builder setProtoRequestInterceptor(@NotNull ProtoRequestInterceptor interceptor) {
            this.protoRequestInterceptor = interceptor;
            return this;
        }


        // ============================= ssl tcp auth =============================== //

        @NotNull
        public Builder setSslEnable(boolean enable) {
            this.sslEnable = enable;
            return this;
        }

        @NotNull
        public Builder setSslContext(SslContext context) {
            this.sslContext = context;
            return this;
        }

        @NotNull
        public Builder setSslBindAddr(String bindAddr) {
            this.sslBindAddr = bindAddr;
            return this;
        }

        @NotNull
        public Builder setSslBindPort(int bindPort) {
            this.sslBindPort = bindPort;
            return this;
        }

        // ============================= http =============================== //

        @NotNull
        public Builder setHttpEnable(boolean enable) {
            this.httpEnable = enable;
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
        public Builder setHttpRequestInterceptor(@NotNull HttpRequestInterceptor interceptor) {
            this.httpRequestInterceptor = interceptor;
            return this;
        }

        // ============================= https =============================== //
        @NotNull
        public Builder setHttpsEnable(boolean enable) {
            this.httpsEnable = enable;
            return this;
        }

        @NotNull
        public Builder setHttpsContext(SslContext context) {
            this.httpsContext = context;
            return this;
        }

        @NotNull
        public Builder setHttpsBindAddr(String bindAddr) {
            this.httpsBindAddr = bindAddr;
            return this;
        }

        @NotNull
        public Builder setHttpsBindPort(int bindPort) {
            this.httpsBindPort = bindPort;
            return this;
        }

        @NotNull
        public Builder setHttpsRequestInterceptor(@NotNull HttpRequestInterceptor interceptor) {
            this.httpsRequestInterceptor = interceptor;
            return this;
        }

        @NotNull
        public TunnelServer build() {
            return new TunnelServer(this);
        }
    }
}
