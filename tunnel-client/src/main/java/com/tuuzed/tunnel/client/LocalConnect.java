package com.tuuzed.tunnel.client;

import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import com.tuuzed.tunnel.common.protocol.TunnelAttributeKey;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


class LocalConnect {
    private static final Logger logger = LoggerFactory.getLogger(LocalConnect.class);

    @NotNull
    private final Map<String, Channel> cachedLocalConnectChannels = new ConcurrentHashMap<>();
    @NotNull
    private final Bootstrap bootstrap;

    public LocalConnect(@NotNull NioEventLoopGroup workerGroup) {
        bootstrap = new Bootstrap();
        bootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast(new LocalConnectChannelHandler(LocalConnect.this))
                        ;
                    }
                });
    }

    public void removeLocalConnectChannel(long tunnelToken, long sessionToken) {
        Channel localTunnelChannel = removeCachedLocalConnectChannel(tunnelToken, sessionToken);
        if (localTunnelChannel != null) {
            localTunnelChannel.close();
        }
    }

    public void getLocalConnectChannel(
            @NotNull final String localAddr, final int localPort,
            final long tunnelToken, final long sessionToken,
            final Channel tunnelClientChannel, @NotNull final GetLocalContentChannelCallback callback) {
        logger.trace("cachedLocalConnectChannels: {}", cachedLocalConnectChannels);
        final Channel localContentChannel = getCachedLocalConnectChannel(tunnelToken, sessionToken);
        if (localContentChannel != null && localContentChannel.isActive()) {
            callback.success(localContentChannel);
        } else {
            bootstrap.connect(localAddr, localPort).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        // 二次检查是否有可用的Channel缓存
                        Channel localContentChannel = getCachedLocalConnectChannel(tunnelToken, sessionToken);
                        if (localContentChannel == null || !localContentChannel.isActive()) {
                            removeLocalConnectChannel(tunnelToken, sessionToken);
                            localContentChannel = future.channel();
                            localContentChannel.attr(TunnelAttributeKey.TUNNEL_TOKEN).set(tunnelToken);
                            localContentChannel.attr(TunnelAttributeKey.SESSION_TOKEN).set(sessionToken);
                            localContentChannel.attr(TunnelAttributeKey.NEXT_CHANNEL).set(tunnelClientChannel);

                            putCachedLocalConnectChannel(tunnelToken, sessionToken, localContentChannel);
                        } else {
                            future.channel().close();
                        }
                        callback.success(localContentChannel);
                    } else {
                        callback.error(future.cause());
                    }
                }
            });
        }
    }

    public void destroy() {
        cachedLocalConnectChannels.clear();
    }

    @Nullable
    private Channel getCachedLocalConnectChannel(long tunnelToken, long sessionToken) {
        final String key = getCachedLocalConnectChannelKey(tunnelToken, sessionToken);
        synchronized (cachedLocalConnectChannels) {
            return cachedLocalConnectChannels.get(key);
        }
    }

    private void putCachedLocalConnectChannel(long tunnelToken, long sessionToken, @NotNull Channel localConnectChannel) {
        final String key = getCachedLocalConnectChannelKey(tunnelToken, sessionToken);
        synchronized (cachedLocalConnectChannels) {
            cachedLocalConnectChannels.put(key, localConnectChannel);
        }
    }

    @Nullable
    private Channel removeCachedLocalConnectChannel(long tunnelToken, long sessionToken) {
        final String key = getCachedLocalConnectChannelKey(tunnelToken, sessionToken);
        synchronized (cachedLocalConnectChannels) {
            return cachedLocalConnectChannels.remove(key);
        }
    }

    @NotNull
    private String getCachedLocalConnectChannelKey(long tunnelToken, long sessionToken) {
        return String.format("%d-%d", tunnelToken, sessionToken);
    }

    public interface GetLocalContentChannelCallback {
        void success(@NotNull Channel localContentChannel);

        void error(Throwable cause);
    }


}
