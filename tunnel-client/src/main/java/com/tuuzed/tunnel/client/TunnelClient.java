package com.tuuzed.tunnel.client;

import com.tuuzed.tunnel.client.internal.AttributeKeys;
import com.tuuzed.tunnel.client.local.LocalConnect;
import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import com.tuuzed.tunnel.common.proto.*;
import com.tuuzed.tunnel.common.util.Function1;
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
    private static final AttributeKey<Descriptor> DESCRIPTOR = AttributeKey.newInstance("$DESCRIPTOR");

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
    private final TunnelClientListener listener;
    /* 是否自动重新连接 */
    private final boolean autoReconnect;
    @NotNull
    private final Function1<Descriptor> failureCallback = new Function1<Descriptor>() {
        @Override
        public void invoke(@NotNull Descriptor descriptor) throws Exception {
            if (!descriptor.isShutdown() && autoReconnect) {
                // 连接失败，3秒后发起重连
                TimeUnit.SECONDS.sleep(3);
                descriptor.connect(this);
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
                final Boolean fatalFlag = ctx.channel().attr(AttributeKeys.FATAL_FLAG).get();
                final Throwable fatalCause = ctx.channel().attr(AttributeKeys.FATAL_CAUSE).get();
                final Descriptor descriptor = ctx.channel().attr(DESCRIPTOR).get();
                if (fatalFlag != null && fatalFlag) {
                    if (descriptor != null) {
                        if (listener != null) {
                            listener.onDisconnect(descriptor, true);
                        }
                    }
                    logger.error("{}", fatalCause.getMessage(), fatalCause);
                } else {
                    if (descriptor != null) {
                        if (listener != null) {
                            listener.onDisconnect(descriptor, false);
                        }
                        if (!descriptor.isShutdown() && autoReconnect) {
                            TimeUnit.SECONDS.sleep(3);
                            descriptor.connect(failureCallback);
                            if (listener != null) {
                                listener.onConnecting(descriptor, true);
                            }
                        }
                    }
                }
            }

            @Override
            public void tunnelConnected(@NotNull ChannelHandlerContext ctx) {
                final Descriptor descriptor = ctx.channel().attr(DESCRIPTOR).get();
                if (listener != null) {
                    listener.onConnected(descriptor);
                }
            }
        };

        bootstrap.group(workerGroup)
            .channel(NioSocketChannel.class)
            .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline()
                        .addLast(new ProtoMessageDecoder())
                        .addLast(new ProtoMessageEncoder())
                        .addLast(new ProtoHeartbeatHandler())
                        .addLast(new TunnelClientChannelHandler(
                            localConnect, channelListener
                        ))
                    ;
                }
            });
    }

    @NotNull
    public Descriptor connect(
        @NotNull final String serverAddr,
        final int serverPort,
        @NotNull final ProtoRequest protoRequest,
        @Nullable final SslContext sslContext
    ) {

        final Descriptor descriptor = (sslContext == null)
            ? new Descriptor(bootstrap, serverAddr, serverPort, protoRequest)
            : new Descriptor(getSslBootstrap(sslContext), serverAddr, serverPort, protoRequest);
        descriptor.connect(failureCallback);
        if (listener != null) {
            listener.onConnecting(descriptor, false);
        }
        return descriptor;
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
                            .addLast(new ProtoMessageDecoder())
                            .addLast(new ProtoMessageEncoder())
                            .addLast(new ProtoHeartbeatHandler())
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

    public void shutdown(@NotNull final Descriptor descriptor) {
        descriptor.shutdown();
    }

    public void destroy() {
        workerGroup.shutdownGracefully();
        localConnect.destroy();
        sslBootstraps.clear();
    }

    public static class Descriptor {
        @NotNull
        private final Bootstrap bootstrap;
        @NotNull
        private final String serverAddr;
        private final int serverPort;
        @NotNull
        private final ProtoRequest protoRequest;
        @NotNull
        private final AtomicBoolean shutdownFlag = new AtomicBoolean(false);
        @Nullable
        private ChannelFuture connectChannelFuture;

        private Descriptor(
            @NotNull final Bootstrap bootstrap,
            @NotNull final String serverAddr,
            final int serverPort,
            @NotNull final ProtoRequest protoRequest
        ) {
            this.bootstrap = bootstrap;
            this.serverAddr = serverAddr;
            this.serverPort = serverPort;
            this.protoRequest = protoRequest;
        }

        private void connect(@NotNull final Function1<Descriptor> failureCallback) {
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
                            new ProtoMessage(
                                ProtoMessage.Type.REQUEST,
                                protoRequest.toBytes(),
                                null
                            )
                        );
                        future.channel().attr(DESCRIPTOR).set(Descriptor.this);
                    } else {
                        failureCallback.invoke(Descriptor.this);
                    }
                }
            });
        }

        @NotNull
        public String serverAddr() {
            return serverAddr;
        }

        public int serverPort() {
            return serverPort;
        }

        @NotNull
        public ProtoRequest protoRequest() {
            return protoRequest;
        }

        public boolean isShutdown() {
            return shutdownFlag.get();
        }

        public void shutdown() {
            if (connectChannelFuture != null) {
                shutdownFlag.set(true);
                connectChannelFuture.channel().attr(DESCRIPTOR).set(null);
                connectChannelFuture.channel().close();
            }
        }

        @Override
        public String toString() {
            return protoRequest.toString();
        }
    }

    public static class Builder {
        @Nullable
        private TunnelClientListener listener;
        private boolean autoReconnect = true;
        private int workerThreads = -1;

        @NotNull
        public Builder setWorkerThreads(int workerThreads) {
            this.workerThreads = workerThreads;
            return this;
        }

        @NotNull
        public Builder setListener(@Nullable TunnelClientListener listener) {
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


}
