package com.tuuzed.tunnel.client;

import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import com.tuuzed.tunnel.common.protocol.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.AttributeKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class TunnelClient {
    private static final Logger logger = LoggerFactory.getLogger(TunnelClient.class);
    private static final AttributeKey<Tunnel> ATTR_TUNNEL = AttributeKey.newInstance("tunnel");

    @NotNull
    private final NioEventLoopGroup workerGroup;
    @NotNull
    private final Bootstrap bootstrap;
    @NotNull
    private final Map<SslContext, Bootstrap> sslBootstraps = new ConcurrentHashMap<>();
    @NotNull
    private final TunnelClientChannelListener tunnelClientChannelListener;
    @NotNull
    private final LocalConnectManager localConnectManager;

    private TunnelClient(@NotNull final Builder builder) {
        this.workerGroup = (builder.workerThreads > 0)
                ? new NioEventLoopGroup(builder.workerThreads)
                : new NioEventLoopGroup();
        this.bootstrap = new Bootstrap();
        this.localConnectManager = new LocalConnectManager(workerGroup);
        this.tunnelClientChannelListener = new TunnelClientChannelListener() {
            @Override
            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                Boolean openTunnelFailFlag = ctx.channel().attr(TunnelAttributeKey.OPEN_TUNNEL_FAIL_FLAG).get();
                String errorMessage = ctx.channel().attr(TunnelAttributeKey.OPEN_TUNNEL_FAIL_MESSAGE).get();
                Tunnel tunnel = ctx.channel().attr(ATTR_TUNNEL).get();
                // 服务器端放回错误响应
                if (openTunnelFailFlag != null && openTunnelFailFlag) {
                    logger.error(errorMessage);
                    return;
                }
                if (tunnel != null) {
                    TimeUnit.SECONDS.sleep(3);
                    tunnel.connect();
                }
            }
        };

        bootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast(new TunnelMessageDecoder())
                                .addLast(new TunnelMessageEncoder())
                                .addLast(new TunnelHeartbeatHandler())
                                .addLast(new TunnelClientChannelHandler(localConnectManager, tunnelClientChannelListener))
                        ;
                    }
                });
    }

    @NotNull
    public Tunnel connect(
            @NotNull final String serverAddr,
            final int serverPort,
            @NotNull final OpenTunnelRequest request,
            @Nullable final SslContext context
    ) {
        Tunnel tunnel = (context == null)
                ? new Tunnel(bootstrap, serverAddr, serverPort, request)
                : new Tunnel(getSslBootstrap(context), serverAddr, serverPort, request);
        tunnel.connect();
        return tunnel;
    }

    @NotNull
    private Bootstrap getSslBootstrap(@NotNull final SslContext context) {
        Bootstrap sslBootstrap = sslBootstraps.get(context);
        if (sslBootstrap == null) {
            sslBootstrap = new Bootstrap();
            sslBootstrap.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast(new SslHandler(context.newEngine(ch.alloc())))
                                    .addLast(new TunnelMessageDecoder())
                                    .addLast(new TunnelMessageEncoder())
                                    .addLast(new TunnelHeartbeatHandler())
                                    .addLast(new TunnelClientChannelHandler(localConnectManager, tunnelClientChannelListener))
                            ;
                        }
                    });
            sslBootstraps.put(context, sslBootstrap);
        }
        return sslBootstrap;
    }

    public void shutdown(@NotNull Tunnel tunnel) {
        tunnel.shutdown();
    }

    public void destroy() {
        workerGroup.shutdownGracefully();
        localConnectManager.destroy();
        sslBootstraps.clear();
    }

    public static class Tunnel {

        private final String serverAddr;
        private final int serverPort;
        private final OpenTunnelRequest request;

        private final Bootstrap bootstrap;

        private final AtomicBoolean stopFlag = new AtomicBoolean(false);
        @Nullable
        private ChannelFuture connectChannelFuture;

        private Tunnel(@NotNull Bootstrap bootstrap, @NotNull String serverAddr, int serverPort, @NotNull OpenTunnelRequest request) {
            this.bootstrap = bootstrap;
            this.serverAddr = serverAddr;
            this.serverPort = serverPort;
            this.request = request;
        }

        private void connect() {
            if (stopFlag.get()) {
                return;
            }
            ChannelFuture f = bootstrap.connect(serverAddr, serverPort);
            connectChannelFuture = f;
            f.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        // 连接成功，向服务器发送请求建立隧道消息
                        future.channel().writeAndFlush(
                                TunnelMessage.newInstance(TunnelMessage.MESSAGE_TYPE_OPEN_TUNNEL_REQUEST)
                                        .setHead(request.toBytes())
                        );
                        future.channel().attr(ATTR_TUNNEL).set(Tunnel.this);
                    } else {
                        // 连接失败，3秒后发起重连
                        TimeUnit.SECONDS.sleep(3);
                        connect();
                    }
                }
            });
        }

        @NotNull
        public String getServerAddr() {
            return serverAddr;
        }

        public int getServerPort() {
            return serverPort;
        }

        @NotNull
        public OpenTunnelRequest getRequest() {
            return request;
        }

        public void shutdown() {
            if (connectChannelFuture != null) {
                stopFlag.set(true);
                connectChannelFuture.channel().attr(ATTR_TUNNEL).set(null);
                connectChannelFuture.channel().close();
            }
        }

    }

    public static class Builder {
        private int workerThreads = -1;

        @NotNull
        public Builder setWorkerThreads(int workerThreads) {
            this.workerThreads = workerThreads;
            return this;
        }

        @NotNull
        public TunnelClient build() {
            return new TunnelClient(this);
        }
    }

}
