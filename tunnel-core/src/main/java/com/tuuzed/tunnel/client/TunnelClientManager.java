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
import io.netty.util.AttributeKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class TunnelClientManager {
    private static final Logger logger = LoggerFactory.getLogger(TunnelClientManager.class);
    private static final AttributeKey<TunnelClientImpl> ATTR_TUNNEL_CLIENT = AttributeKey.newInstance("tunnel_client");

    private static class InstanceHolder {
        private static final TunnelClientManager instance = new TunnelClientManager();
    }

    @NotNull
    public static TunnelClientManager getInstance() {
        return InstanceHolder.instance;
    }

    @NotNull
    private final NioEventLoopGroup workerGroup;
    @NotNull
    private final Bootstrap bootstrap;

    private TunnelClientManager() {
        this.workerGroup = new NioEventLoopGroup();
        this.bootstrap = new Bootstrap();
        final TunnelClientChannelListener listener = new TunnelClientChannelListener() {
            @Override
            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                Boolean openTunnelFailFlag = ctx.channel().attr(TunnelAttributeKey.OPEN_TUNNEL_FAIL_FLAG).get();
                String errorMessage = ctx.channel().attr(TunnelAttributeKey.OPEN_TUNNEL_FAIL_MESSAGE).get();
                TunnelClientImpl tunnelClient = ctx.channel().attr(ATTR_TUNNEL_CLIENT).get();
                if (openTunnelFailFlag != null && openTunnelFailFlag) {
                    logger.error(errorMessage);
                    return;
                }
                if (tunnelClient != null) {
                    TimeUnit.SECONDS.sleep(3);
                    tunnelClient.connect(bootstrap);
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
                                .addLast(new TunnelClientChannelHandler(listener))
                        ;
                    }
                });
    }

    @NotNull
    public TunnelClient connect(@NotNull final String serverAddr, final int serverPort, @NotNull final OpenTunnelRequest request) {
        final TunnelClientImpl client = new TunnelClientImpl(serverAddr, serverPort, request);
        client.connect(bootstrap);
        return client;
    }

    public void shutdown(@NotNull TunnelClient client) {
        client.shutdown();
    }

    private static class TunnelClientImpl implements TunnelClient {
        private static final Logger logger = LoggerFactory.getLogger(TunnelClientImpl.class);

        private final String serverAddr;
        private final int serverPort;
        private final OpenTunnelRequest request;

        private final AtomicBoolean stopFlag = new AtomicBoolean(false);
        @Nullable
        private ChannelFuture connectChannelFuture;

        private TunnelClientImpl(@NotNull String serverAddr, int serverPort, @NotNull OpenTunnelRequest request) {
            this.serverAddr = serverAddr;
            this.serverPort = serverPort;
            this.request = request;
        }

        private void connect(@NotNull final Bootstrap bootstrap) {
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
                        future.channel().attr(ATTR_TUNNEL_CLIENT).set(TunnelClientImpl.this);
                        logger.debug("connect tunnel server success, {}", future.channel());
                    } else {
                        logger.debug("connect tunnel server failed, {}", future.channel(), future.cause());
                        // 连接失败，3秒后发起重连
                        TimeUnit.SECONDS.sleep(3);
                        connect(bootstrap);
                    }
                }
            });
        }

        @NotNull
        @Override
        public String getServerAddr() {
            return serverAddr;
        }

        @Override
        public int getServerPort() {
            return serverPort;
        }

        @NotNull
        @Override
        public OpenTunnelRequest getRequest() {
            return request;
        }

        @Override
        public void shutdown() {
            if (connectChannelFuture != null) {
                stopFlag.set(true);
                connectChannelFuture.channel().attr(ATTR_TUNNEL_CLIENT).set(null);
                connectChannelFuture.channel().close();
            }
        }

    }

}
