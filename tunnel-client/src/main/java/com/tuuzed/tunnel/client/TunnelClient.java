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
    private static final AttributeKey<TunnelDescriptor> ATTR_TUNNEL_DESCRIPTOR = AttributeKey.newInstance("tunnel_descriptor");

    @NotNull
    private final NioEventLoopGroup workerGroup;
    @NotNull
    private final Bootstrap bootstrap;
    @NotNull
    private final Map<SslContext, Bootstrap> sslBootstraps = new ConcurrentHashMap<>();
    @NotNull
    private final TunnelClientChannelHandler.ChannelListener channelListener;
    @NotNull
    private final LocalConnect localConnect;
    @Nullable
    private final Listener listener;
    /* 是否自动重新连接 */
    private final boolean autoReconnect;
    @NotNull
    private final TunnelDescriptorConnectFailureCallback tunnelDescriptorConnectFailureCallback = new TunnelDescriptorConnectFailureCallback() {
        @Override
        public void invoke(@NotNull TunnelDescriptor tunnelDescriptor) throws Exception {
            if (!tunnelDescriptor.hasShutdown() && autoReconnect) {
                // 连接失败，3秒后发起重连
                TimeUnit.SECONDS.sleep(3);
                tunnelDescriptor.connect(this);
            }
        }
    };

    private TunnelClient(@NotNull final Builder builder) {
        this.workerGroup = (builder.workerThreads > 0)
                ? new NioEventLoopGroup(builder.workerThreads)
                : new NioEventLoopGroup();
        this.bootstrap = new Bootstrap();
        this.localConnect = new LocalConnect(workerGroup);
        this.listener = builder.listener;
        this.autoReconnect = builder.autoReconnect;
        this.channelListener = new TunnelClientChannelHandler.ChannelListener() {
            @Override
            public void channelInactive(@NotNull ChannelHandlerContext ctx) throws Exception {
                Boolean openTunnelFailFlag = ctx.channel().attr(TunnelAttributeKey.OPEN_TUNNEL_FAIL_FLAG).get();
                String errorMessage = ctx.channel().attr(TunnelAttributeKey.OPEN_TUNNEL_FAIL_MESSAGE).get();
                TunnelDescriptor tunnelDescriptor = ctx.channel().attr(ATTR_TUNNEL_DESCRIPTOR).get();
                // 服务器端放回错误响应
                if (openTunnelFailFlag != null && openTunnelFailFlag) {
                    if (tunnelDescriptor != null) {
                        if (listener != null) {
                            listener.onDisconnect(tunnelDescriptor, true);
                        }
                    }
                    logger.error(errorMessage);
                } else {
                    if (tunnelDescriptor != null) {
                        if (listener != null) {
                            listener.onDisconnect(tunnelDescriptor, false);
                        }
                        if (!tunnelDescriptor.hasShutdown() && autoReconnect) {
                            TimeUnit.SECONDS.sleep(3);
                            tunnelDescriptor.connect(tunnelDescriptorConnectFailureCallback);
                            if (listener != null) {
                                listener.onConnecting(tunnelDescriptor, true);
                            }
                        }
                    }
                }
            }

            @Override
            public void tunnelConnected(@NotNull ChannelHandlerContext ctx) {
                final TunnelDescriptor tunnelDescriptor = ctx.channel().attr(ATTR_TUNNEL_DESCRIPTOR).get();
                if (listener != null) {
                    listener.onConnected(tunnelDescriptor);
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
                                .addLast(new TunnelClientChannelHandler(
                                        localConnect, channelListener
                                ))
                        ;
                    }
                });
    }

    @NotNull
    public TunnelDescriptor connect(
            @NotNull final String serverAddr,
            final int serverPort,
            @NotNull final OpenTunnelRequest request,
            @Nullable final SslContext context
    ) {
        TunnelDescriptor tunnel = (context == null)
                ? new TunnelDescriptor(bootstrap, serverAddr, serverPort, request)
                : new TunnelDescriptor(getSslBootstrap(context), serverAddr, serverPort, request);
        tunnel.connect(tunnelDescriptorConnectFailureCallback);
        if (listener != null) {
            listener.onConnecting(tunnel, false);
        }
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
                                    .addLast(new TunnelClientChannelHandler(
                                            localConnect, channelListener
                                    ))
                            ;
                        }
                    });
            sslBootstraps.put(context, sslBootstrap);
        }
        return sslBootstrap;
    }

    public void shutdown(@NotNull final TunnelDescriptor tunnelDescriptor) {
        tunnelDescriptor.shutdown();
    }

    public void destroy() {
        workerGroup.shutdownGracefully();
        localConnect.destroy();
        sslBootstraps.clear();
    }

    private interface TunnelDescriptorConnectFailureCallback {
        void invoke(@NotNull TunnelDescriptor tunnel) throws Exception;
    }

    public static class TunnelDescriptor {
        @NotNull
        private final String serverAddr;
        private final int serverPort;
        @NotNull
        private final OpenTunnelRequest request;
        @NotNull
        private final Bootstrap bootstrap;
        @NotNull
        private final AtomicBoolean shutdownFlag = new AtomicBoolean(false);
        @Nullable
        private ChannelFuture connectChannelFuture;

        private TunnelDescriptor(
                @NotNull final Bootstrap bootstrap,
                @NotNull final String serverAddr,
                final int serverPort,
                @NotNull final OpenTunnelRequest request
        ) {
            this.bootstrap = bootstrap;
            this.serverAddr = serverAddr;
            this.serverPort = serverPort;
            this.request = request;
        }

        private void connect(@NotNull final TunnelDescriptorConnectFailureCallback callback) {
            if (shutdownFlag.get()) {
                logger.warn("This tunnel already shutdown.");
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
                        future.channel().attr(ATTR_TUNNEL_DESCRIPTOR).set(TunnelDescriptor.this);
                    } else {
                        callback.invoke(TunnelDescriptor.this);
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

        public boolean hasShutdown() {
            return shutdownFlag.get();
        }

        public void shutdown() {
            if (connectChannelFuture != null) {
                shutdownFlag.set(true);
                connectChannelFuture.channel().attr(ATTR_TUNNEL_DESCRIPTOR).set(null);
                connectChannelFuture.channel().close();
            }
        }

        @Override
        public String toString() {
            return request.toString();
        }
    }

    public static class Builder {
        @Nullable
        private Listener listener;
        private boolean autoReconnect = true;
        private int workerThreads = -1;

        @NotNull
        public Builder setWorkerThreads(int workerThreads) {
            this.workerThreads = workerThreads;
            return this;
        }

        @NotNull
        public Builder setListener(@Nullable Listener listener) {
            this.listener = listener;
            return this;
        }

        @NotNull
        public Builder setAutoReconnect(boolean autoReconnect) {
            this.autoReconnect = autoReconnect;
            return this;
        }

        @NotNull
        public TunnelClient build() {
            return new TunnelClient(this);
        }
    }

    public interface Listener {
        void onConnecting(@NotNull TunnelDescriptor tunnelDescriptor, boolean reconnect);

        void onConnected(@NotNull TunnelDescriptor tunnelDescriptor);

        void onDisconnect(@NotNull TunnelDescriptor tunnelDescriptor, boolean deadly);
    }
}
