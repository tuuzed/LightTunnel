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

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.tuuzed.tunnel.common.protocol.TunnelConstants.*;

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
    private final Map<String, String> options;


    public TunnelClient(
            @NotNull String serverAddr, int serverPort,
            @NotNull String localAddr, int localPort,
            int remotePort
    ) {
        this(serverAddr, serverPort, localAddr, localPort, remotePort, Collections.<String, String>emptyMap());
    }

    public TunnelClient(
            @NotNull String serverAddr, int serverPort,
            @NotNull String localAddr, int localPort,
            int remotePort,
            @NotNull Map<String, String> options
    ) {
        this.bootstrap = new Bootstrap();
        this.workerGroup = new NioEventLoopGroup();
        this.serverAddr = serverAddr;
        this.serverPort = serverPort;
        this.localAddr = localAddr;
        this.localPort = localPort;
        this.remotePort = remotePort;
        this.options = options;
        bootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast(new TunnelMessageDecoder())
                                .addLast(new TunnelMessageEncoder())
                                .addLast(new TunnelHeartbeatHandler())
                                .addLast(new TunnelClientChannelHandler()
                                        .setTunnelClientChannelListener(new TunnelClientChannelListener() {
                                            @Override
                                            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                                // 连接断开，3秒后发起重连
                                                TimeUnit.SECONDS.sleep(3);
                                                start();
                                            }
                                        })
                                )
                        ;
                    }
                });
    }

    @NotNull
    public ChannelFuture start() {
        return connectTunnelServerAndRequestOpenTunnel();
    }

    public void stop() {
        workerGroup.shutdownGracefully();
    }

    @NotNull
    @SuppressWarnings("Duplicates")
    private ChannelFuture connectTunnelServerAndRequestOpenTunnel() {
        ChannelFuture f = bootstrap.connect(serverAddr, serverPort);
        f.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    OpenTunnelRequest openTunnelRequest = new OpenTunnelRequest(
                            OpenTunnelRequest.SCHEME_TCP, localAddr, localPort, remotePort, options
                    );
                    // 连接成功，向服务器发送请求建立隧道消息
                    future.channel().attr(ATTR_OPEN_TUNNEL_REQUEST).set(openTunnelRequest);

                    future.channel().writeAndFlush(
                            TunnelMessage
                                    .newInstance(MESSAGE_TYPE_OPEN_TUNNEL_REQUEST)
                                    .setHead(openTunnelRequest.toString().getBytes())
                    );
                    logger.info("connect tunnel server success, {}", future.channel());
                } else {
                    logger.warn("connect tunnel server failed {}", future.channel(), future.cause());
                    // 连接失败，3秒后发起重连
                    TimeUnit.SECONDS.sleep(3);
                    connectTunnelServerAndRequestOpenTunnel();
                }
            }
        });
        return f;
    }

}
