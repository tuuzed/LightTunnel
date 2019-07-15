package com.tuuzed.tunnel.client;

import com.tuuzed.tunnel.common.handler.TunnelHeartbeatHandler;
import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import com.tuuzed.tunnel.common.protocol.OpenTunnelRequest;
import com.tuuzed.tunnel.common.protocol.TunnelMessage;
import com.tuuzed.tunnel.common.protocol.TunnelMessageDecoder;
import com.tuuzed.tunnel.common.protocol.TunnelMessageEncoder;
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

import static com.tuuzed.tunnel.common.protocol.TunnelConstants.*;

@SuppressWarnings("Duplicates")
public class TunnelClient {
    private static final Logger logger = LoggerFactory.getLogger(TunnelClient.class);
    private static final AttributeKey<TunnelClient> ATTR_TUNNEL_CLIENT = AttributeKey.newInstance("tunnel_client");
    @NotNull
    private final static Bootstrap bootstrap = new Bootstrap();
    @NotNull
    private final static NioEventLoopGroup workerGroup = new NioEventLoopGroup();

    static {
        final TunnelClientChannelListener listener = new TunnelClientChannelListener() {
            @Override
            public void channelInactive(ChannelHandlerContext ctx) throws Exception {

                Boolean openTunnelError = ctx.channel().attr(ATTR_OPEN_TUNNEL_ERROR_FLAG).get();
                String errorMessage = ctx.channel().attr(ATTR_OPEN_TUNNEL_ERROR_MESSAGE).get();
                TunnelClient tunnelClient = ctx.channel().attr(ATTR_TUNNEL_CLIENT).get();
                logger.debug("channelInactive: {}, openTunnelError: {}, errorMessage: {}", ctx, openTunnelError, errorMessage);
                if (openTunnelError != null && openTunnelError) {
                    logger.error(errorMessage);
                    return;
                }
                if (tunnelClient != null) {
                    TimeUnit.SECONDS.sleep(3);
                    tunnelClient.start();
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

    private final String serverAddr;
    private final int serverPort;
    private final OpenTunnelRequest request;
    @Nullable
    private ChannelFuture connectChannelFuture;


    public TunnelClient(
            @NotNull String serverAddr, int serverPort,
            OpenTunnelRequest request
    ) {
        this.serverAddr = serverAddr;
        this.serverPort = serverPort;
        this.request = request;

    }

    public void start() {
        ChannelFuture f = bootstrap.connect(serverAddr, serverPort);
        connectChannelFuture = f;
        f.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    // 连接成功，向服务器发送请求建立隧道消息
                    future.channel().writeAndFlush(
                            TunnelMessage
                                    .newInstance(MESSAGE_TYPE_OPEN_TUNNEL_REQUEST)
                                    .setHead(request.toBytes())
                    );
                    future.channel().attr(ATTR_TUNNEL_CLIENT).set(TunnelClient.this);
                    logger.debug("connect tunnel server success, {}", future.channel());
                } else {
                    logger.debug("connect tunnel server failed, {}", future.channel(), future.cause());
                    // 连接失败，3秒后发起重连
                    TimeUnit.SECONDS.sleep(3);
                    start();
                }
            }
        });
    }

    public void stop() {
        if (connectChannelFuture != null) {
            connectChannelFuture.channel().close();
        }
    }

}
