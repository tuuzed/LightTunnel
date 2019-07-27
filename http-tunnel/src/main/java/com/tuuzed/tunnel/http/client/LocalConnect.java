package com.tuuzed.tunnel.http.client;

import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import com.tuuzed.tunnel.common.protocol.TunnelAttributeKey;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LocalConnect {
    private static final Logger logger = LoggerFactory.getLogger(LocalConnect.class);

    private final Map<String, Channel> cachedChannels = new ConcurrentHashMap<>();
    private final Bootstrap bootstrap;

    public LocalConnect(@NotNull NioEventLoopGroup workerGroup) {
        bootstrap = new Bootstrap();
        bootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new LocalConnectChannelHandler() {
                            @Override
                            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                final Long tunnelToken = ctx.channel().attr(TunnelAttributeKey.TUNNEL_TOKEN).get();
                                final Long sessionToken = ctx.channel().attr(TunnelAttributeKey.SESSION_TOKEN).get();
                                if (tunnelToken != null && sessionToken != null) {
                                    synchronized (cachedChannels) {
                                        cachedChannels.remove(getCachedChannelKey(tunnelToken, sessionToken));
                                    }
                                }
                                super.channelInactive(ctx);
                            }
                        });
                    }
                });
    }

    public void writeDataToLocal(
            @NotNull final Channel nextChannel,
            final long tunnelToken,
            final long sessionToken,
            final String localAddr,
            final int localPort,
            @NotNull final ByteBuf data
    ) {
        logger.debug("cachedChannels: {}", cachedChannels);
        final Channel channel;
        synchronized (cachedChannels) {
            channel = cachedChannels.get(getCachedChannelKey(tunnelToken, sessionToken));
        }
        if (channel != null) {
            if (channel.isActive()) {
                channel.writeAndFlush(data);
            } else {
                cachedChannels.remove(getCachedChannelKey(tunnelToken, sessionToken));
            }
        } else {
            bootstrap.connect(localAddr, localPort).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    final Channel channel;
                    synchronized (cachedChannels) { // 二次检查
                        channel = cachedChannels.get(getCachedChannelKey(tunnelToken, sessionToken));
                        if (channel != null && channel.isActive()) {
                            channel.writeAndFlush(data);
                            future.channel().close();
                        } else {
                            cachedChannels.remove(getCachedChannelKey(tunnelToken, sessionToken));
                            if (future.isSuccess()) {
                                future.channel().attr(TunnelAttributeKey.NEXT_CHANNEL).set(nextChannel);
                                future.channel().attr(TunnelAttributeKey.TUNNEL_TOKEN).set(tunnelToken);
                                future.channel().attr(TunnelAttributeKey.SESSION_TOKEN).set(sessionToken);
                                future.channel().writeAndFlush(data);
                                cachedChannels.put(getCachedChannelKey(tunnelToken, sessionToken), future.channel());
                            }
                        }
                    }
                }
            });
        }
    }

    private String getCachedChannelKey(long tunnelToken, long sessionToken) {
        return String.format("%d-%d", tunnelToken, sessionToken);
    }


}
