package com.tuuzed.tunnel.client;

import com.tuuzed.tunnel.client.internal.AttributeKeys;
import com.tuuzed.tunnel.client.local.LocalConnect;
import com.tuuzed.tunnel.common.proto.ProtoRequest;
import com.tuuzed.tunnel.common.util.Function1;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class TunnelClient {
    private static final Logger logger = LoggerFactory.getLogger(TunnelClient.class);
    @NotNull
    private final Map<SslContext, Bootstrap> cachedSslBootstraps = new ConcurrentHashMap<>();
    @NotNull
    private final NioEventLoopGroup workerGroup;
    @NotNull
    private final Bootstrap bootstrap;
    @NotNull
    private final TunnelClientChannelHandler.ChannelListener channelListener;
    @NotNull
    private final LocalConnect localConnect;
    @Nullable
    private final TunnelClientListener listener;
    /* 是否自动重新连接 */
    private final boolean autoReconnect;
    @NotNull
    private final Function1<TunnelClientDescriptor> failureCallback = new Function1<TunnelClientDescriptor>() {
        @Override
        public void invoke(@NotNull TunnelClientDescriptor descriptor) throws Exception {
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
                final TunnelClientDescriptor descriptor = ctx.channel().attr(AttributeKeys.TUNNEL_CLIENT_DESCRIPTOR).get();
                if (fatalFlag != null && fatalFlag) {
                    if (descriptor != null) {
                        if (listener != null) {
                            listener.onDisconnect(descriptor, true);
                        }
                    }
                    logger.debug("{}", fatalCause.getMessage(), fatalCause);
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
                final TunnelClientDescriptor descriptor = ctx.channel().attr(AttributeKeys.TUNNEL_CLIENT_DESCRIPTOR).get();
                if (listener != null) {
                    listener.onConnected(descriptor);
                }
            }
        };

        bootstrap
            .group(workerGroup)
            .channel(NioSocketChannel.class)
            .option(ChannelOption.AUTO_READ, true)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .handler(new TunnelClientChannelInitializer(null, localConnect, channelListener));
    }


    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    @NotNull
    public TunnelClientDescriptor connect(
        @NotNull final String serverAddr, final int serverPort,
        @NotNull final ProtoRequest protoRequest,
        @Nullable final SslContext sslContext
    ) {

        final TunnelClientDescriptor descriptor = (sslContext == null)
            ? new TunnelClientDescriptor(bootstrap, serverAddr, serverPort, protoRequest)
            : new TunnelClientDescriptor(getSslBootstrap(sslContext), serverAddr, serverPort, protoRequest);
        descriptor.connect(failureCallback);
        if (listener != null) {
            listener.onConnecting(descriptor, false);
        }
        return descriptor;
    }

    public void shutdown(@NotNull final TunnelClientDescriptor descriptor) {
        descriptor.shutdown();
    }

    public void destroy() {
        workerGroup.shutdownGracefully();
        localConnect.destroy();
        cachedSslBootstraps.clear();
    }

    @NotNull
    private Bootstrap getSslBootstrap(@NotNull final SslContext sslContext) {
        Bootstrap cachedSslBootstrap = cachedSslBootstraps.get(sslContext);
        if (cachedSslBootstrap == null) {
            cachedSslBootstrap = new Bootstrap();
            cachedSslBootstrap
                .group(workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.AUTO_READ, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new TunnelClientChannelInitializer(sslContext, localConnect, channelListener));
            cachedSslBootstraps.put(sslContext, cachedSslBootstrap);
        }
        return cachedSslBootstrap;
    }


    public static class Builder {
        @Nullable
        private TunnelClientListener listener;
        private boolean autoReconnect = true;
        private int workerThreads = -1;

        private Builder() {
        }

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
