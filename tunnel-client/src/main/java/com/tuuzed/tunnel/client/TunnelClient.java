package com.tuuzed.tunnel.client;

import com.tuuzed.tunnel.client.internal.AttributeKeys;
import com.tuuzed.tunnel.client.local.LocalConnect;
import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import com.tuuzed.tunnel.common.proto.ProtoHeartbeatHandler;
import com.tuuzed.tunnel.common.proto.ProtoMessageDecoder;
import com.tuuzed.tunnel.common.proto.ProtoMessageEncoder;
import com.tuuzed.tunnel.common.proto.ProtoRequest;
import com.tuuzed.tunnel.common.util.Function1;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class TunnelClient {
    private static final Logger logger = LoggerFactory.getLogger(TunnelClient.class);


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

    TunnelClient(@NotNull final TunnelClientBuilder builder) {
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
    public static TunnelClientBuilder builder() {
        return new TunnelClientBuilder();
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

    @NotNull
    private Bootstrap getSslBootstrap(@NotNull final SslContext sslContext) {
        Bootstrap sslBootstrap = sslBootstraps.get(sslContext);
        if (sslBootstrap == null) {
            sslBootstrap = new Bootstrap();
            sslBootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                            .addLast(new SslHandler(sslContext.newEngine(ch.alloc())))
                            .addLast(new ProtoMessageDecoder())
                            .addLast(new ProtoMessageEncoder())
                            .addLast(new ProtoHeartbeatHandler())
                            .addLast(new TunnelClientChannelHandler(
                                localConnect, channelListener
                            ))
                        ;
                    }
                });
            sslBootstraps.put(sslContext, sslBootstrap);
        }
        return sslBootstrap;
    }

    public void shutdown(@NotNull final TunnelClientDescriptor descriptor) {
        descriptor.shutdown();
    }

    public void destroy() {
        workerGroup.shutdownGracefully();
        localConnect.destroy();
        sslBootstraps.clear();
    }


}
