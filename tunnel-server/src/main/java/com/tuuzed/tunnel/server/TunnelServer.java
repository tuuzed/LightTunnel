package com.tuuzed.tunnel.server;

import com.tuuzed.tunnel.interceptor.HttpRequestInterceptor;
import com.tuuzed.tunnel.interceptor.ProtoRequestInterceptor;
import com.tuuzed.tunnel.server.http.HttpTunnelRegistry;
import com.tuuzed.tunnel.server.http.HttpTunnelServer;
import com.tuuzed.tunnel.server.internal.TokenProducer;
import com.tuuzed.tunnel.server.tcp.TcpTunnelRegistry;
import com.tuuzed.tunnel.server.tcp.TcpTunnelServer;
import com.tuuzed.tunnel.server.tcp.TcpTunnelStats;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
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
    @NotNull
    private final HttpRequestInterceptor httpRequestInterceptor;
    @NotNull
    private final HttpRequestInterceptor httpsRequestInterceptor;
    @NotNull
    private final TokenProducer tunnelTokenProducer;

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

    // https
    private final boolean httpsEnable;
    @Nullable
    private final SslContext httpsContext;
    @Nullable
    private final String httpsBindAddr;
    private final int httpsBindPort;

    @Nullable
    private TcpTunnelServer tcpTunnelServer = null;
    @Nullable
    private HttpTunnelServer httpTunnelServer = null;
    @Nullable
    private HttpTunnelServer httpsTunnelServer = null;

    private TunnelServer(@NotNull Builder builder) {
        this.bossGroup = (builder.bossThreads > 0)
            ? new NioEventLoopGroup(builder.bossThreads)
            : new NioEventLoopGroup();
        this.workerGroup = (builder.workerThreads > 0)
            ? new NioEventLoopGroup(builder.workerThreads)
            : new NioEventLoopGroup();

        this.protoRequestInterceptor = builder.protoRequestInterceptor;
        this.httpRequestInterceptor = builder.httpRequestInterceptor;
        this.httpsRequestInterceptor = builder.httpsRequestInterceptor;
        this.tunnelTokenProducer = new TokenProducer();
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
        // https
        this.httpsEnable = builder.httpsEnable;
        this.httpsContext = builder.httpsContext;
        this.httpsBindAddr = builder.httpsBindAddr;
        this.httpsBindPort = builder.httpsBindPort;
    }

    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    public void start() throws Exception {
        final TcpTunnelServer tcpTunnelServer = new TcpTunnelServer(bossGroup, workerGroup);
        this.tcpTunnelServer = tcpTunnelServer;
        if (httpEnable) {
            this.httpTunnelServer = new HttpTunnelServer(
                bossGroup, workerGroup,
                httpBindAddr, httpBindPort,
                null, httpRequestInterceptor
            );
        }
        if (httpsEnable) {
            this.httpsTunnelServer = new HttpTunnelServer(
                bossGroup, workerGroup,
                httpsBindAddr, httpsBindPort,
                httpsContext, httpsRequestInterceptor
            );
        }
        serve(null, tcpTunnelServer);
        if (sslEnable && sslContext != null) serve(sslContext, tcpTunnelServer);
        if (httpTunnelServer != null) httpTunnelServer.start();
        if (httpsTunnelServer != null) httpsTunnelServer.start();
    }

    private void serve(@Nullable SslContext sslContext, @NotNull TcpTunnelServer tcpTunnelServer) throws Exception {
        final ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .childOption(ChannelOption.AUTO_READ, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(new TunnelServerChannelInitializer(
                sslContext,
                tcpTunnelServer, httpTunnelServer, httpsTunnelServer,
                protoRequestInterceptor, tunnelTokenProducer
            ));
        if (sslContext == null) {
            if (bindAddr == null) {
                serverBootstrap.bind(bindPort).get();
            } else {
                serverBootstrap.bind(bindAddr, bindPort).get();
            }
            logger.info("Serving tunnel on {} port {}",
                (bindAddr == null) ? "any address" : bindAddr,
                bindPort
            );

        } else {
            if (sslBindAddr == null) {
                serverBootstrap.bind(sslBindPort).get();
            } else {
                serverBootstrap.bind(sslBindAddr, sslBindPort).get();
            }
            logger.info("Serving ssl tunnel on {} port {}",
                (sslBindAddr == null) ? "any address" : sslBindAddr,
                sslBindPort
            );

        }
    }


    public void destroy() {
        if (tcpTunnelServer != null) tcpTunnelServer.destroy();
        if (httpTunnelServer != null) httpTunnelServer.destroy();
        if (httpsTunnelServer != null) httpsTunnelServer.destroy();
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }


    /**
     * TCP 隧道统计
     *
     * @return
     */
    @Nullable
    public TcpTunnelStats tcpTunnelStats() {
        return (tcpTunnelServer != null) ? tcpTunnelServer.stats() : null;
    }

    /**
     * TCP 隧道注册中心
     *
     * @return
     */
    @Nullable
    public TcpTunnelRegistry tcpTunnelRegistry() {
        return (tcpTunnelServer != null) ? tcpTunnelServer.registry() : null;
    }

    /**
     * HTTP 隧道注册中心
     *
     * @return
     */
    @Nullable
    public HttpTunnelRegistry httpTunnelRegistry() {
        return (httpTunnelServer != null) ? httpTunnelServer.registry() : null;
    }


    /**
     * HTTPS 隧道注册中心
     *
     * @return
     */
    @Nullable
    public HttpTunnelRegistry httpsTunnelRegistry() {
        return (httpsTunnelServer != null) ? httpsTunnelServer.registry() : null;
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
