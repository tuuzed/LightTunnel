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
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.tuuzed.tunnel.common.protocol.TunnelConstants.*;

@SuppressWarnings("Duplicates")
public class TunnelClient {
    private static final Logger logger = LoggerFactory.getLogger(TunnelClient.class);

    private final Bootstrap bootstrap;
    @NotNull
    private final NioEventLoopGroup workerGroup;

    private final String serverAddr;
    private final int serverPort;
    private final String localAddr;
    private final int localPort;
    private final int remotePort;
    private final Map<String, String> arguments;


    public TunnelClient(
            @NotNull String serverAddr, int serverPort,
            @NotNull String localAddr, int localPort,
            int remotePort,
            @NotNull Map<String, String> arguments
    ) {
        this.bootstrap = new Bootstrap();
        this.workerGroup = new NioEventLoopGroup();
        this.serverAddr = serverAddr;
        this.serverPort = serverPort;
        this.localAddr = localAddr;
        this.localPort = localPort;
        this.remotePort = remotePort;
        this.arguments = arguments;
        final TunnelClientChannelListener listener = new TunnelClientChannelListener() {
            @Override
            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                //
                Boolean openTunnelError = ctx.channel().attr(ATTR_OPEN_TUNNEL_ERROR_FLAG).get();
                String errorMessage = ctx.channel().attr(ATTR_OPEN_TUNNEL_ERROR_MESSAGE).get();
                if (openTunnelError != null && openTunnelError) {
                    logger.error(errorMessage);
                    return;
                }
                TimeUnit.SECONDS.sleep(3);
                start();
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
    public ChannelFuture start() {
        ChannelFuture f = bootstrap.connect(serverAddr, serverPort);
        f.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    OpenTunnelRequest openTunnelRequest = new OpenTunnelRequest(
                            OpenTunnelRequest.TYPE_TCP, localAddr, localPort, remotePort, arguments
                    );
                    // 连接成功，向服务器发送请求建立隧道消息
                    future.channel().writeAndFlush(
                            TunnelMessage
                                    .newInstance(MESSAGE_TYPE_OPEN_TUNNEL_REQUEST)
                                    .setHead(openTunnelRequest.toBytes())
                    );
                    logger.info("connect tunnel server success, {}", future.channel());
                } else {
                    logger.warn("connect tunnel server failed {}", future.channel(), future.cause());
                    // 连接失败，3秒后发起重连
                    TimeUnit.SECONDS.sleep(3);
                    start();
                }
            }
        });
        return f;
    }

    public void stop() {
        workerGroup.shutdownGracefully();
    }

}
