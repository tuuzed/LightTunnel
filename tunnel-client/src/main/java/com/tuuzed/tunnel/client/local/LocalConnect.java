package com.tuuzed.tunnel.client.local;

import com.tuuzed.tunnel.client.internal.AttributeKeys;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class LocalConnect {
    private static final Logger logger = LoggerFactory.getLogger(LocalConnect.class);

    @NotNull
    private final Map<String, Channel> cachedChannels = new ConcurrentHashMap<>();
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

    @Nullable
    public Channel removeLocalChannel(long tunnelToken, long sessionToken) {
        return removeCachedChannelSync(tunnelToken, sessionToken);
    }

    public void acquireLocalChannel(
        @NotNull final String localAddr, final int localPort,
        final long tunnelToken, final long sessionToken,
        final Channel tunnelClientChannel, @NotNull final GetLocalContentChannelCallback callback) {
        logger.trace("cachedChannels: {}", cachedChannels);
        final Channel localChannel = getCachedChannelSync(tunnelToken, sessionToken);
        if (localChannel != null && localChannel.isActive()) {
            callback.success(localChannel);
            return;
        }
        bootstrap.connect(localAddr, localPort).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                // 二次检查是否有可用的Channel缓存
                final Channel localChannel = getCachedChannelSync(tunnelToken, sessionToken);
                if (localChannel != null && localChannel.isActive()) {
                    callback.success(localChannel);
                    future.channel().close();
                    return;
                }
                removeLocalChannel(tunnelToken, sessionToken);
                if (future.isSuccess()) {
                    future.channel().attr(AttributeKeys.TUNNEL_TOKEN).set(tunnelToken);
                    future.channel().attr(AttributeKeys.SESSION_TOKEN).set(sessionToken);
                    future.channel().attr(AttributeKeys.NEXT_CHANNEL).set(tunnelClientChannel);
                    putCachedChannelSync(tunnelToken, sessionToken, future.channel());
                    callback.success(future.channel());
                } else {
                    callback.error(future.cause());
                }
            }
        });
    }

    public void destroy() {
        cachedChannels.clear();
    }

    @Nullable
    private Channel getCachedChannelSync(long tunnelToken, long sessionToken) {
        final String key = getCachedChannelKey(tunnelToken, sessionToken);
        synchronized (cachedChannels) {
            return cachedChannels.get(key);
        }
    }

    private void putCachedChannelSync(long tunnelToken, long sessionToken, @NotNull Channel channel) {
        final String key = getCachedChannelKey(tunnelToken, sessionToken);
        synchronized (cachedChannels) {
            cachedChannels.put(key, channel);
        }
    }

    @Nullable
    private Channel removeCachedChannelSync(long tunnelToken, long sessionToken) {
        final String key = getCachedChannelKey(tunnelToken, sessionToken);
        synchronized (cachedChannels) {
            return cachedChannels.remove(key);
        }
    }

    @NotNull
    private String getCachedChannelKey(long tunnelToken, long sessionToken) {
        return String.format("%d-%d", tunnelToken, sessionToken);
    }

    public interface GetLocalContentChannelCallback {
        void success(@NotNull Channel localContentChannel);

        void error(Throwable cause);
    }


}
