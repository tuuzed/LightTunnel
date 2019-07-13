package com.tuuzed.tunnel.client;

import com.tuuzed.tunnel.common.logging.Logger;
import com.tuuzed.tunnel.common.logging.LoggerFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.tuuzed.tunnel.common.protocol.TunnelConstants.*;


public class LocalTunnelChannelManager {

    private static final Logger logger = LoggerFactory.getLogger(LocalTunnelChannelManager.class);

    private static class InstanceHolder {
        private static final LocalTunnelChannelManager instance = new LocalTunnelChannelManager();
    }

    @NotNull
    public static LocalTunnelChannelManager getInstance() {
        return InstanceHolder.instance;
    }

    private final Map<String, Channel> tunnelTokenSessionTokenLocalTunnelChannels = new ConcurrentHashMap<>();

    private final Bootstrap bootstrap;

    private LocalTunnelChannelManager() {
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast(new LocalTunnelChannelHandler())
                        ;
                    }
                });
    }


    public void removeLocalTunnelChannel(final long tunnelToken, final long sessionToken) {
        final String key = String.format("%d@%d", tunnelToken, sessionToken);
        Channel localTunnelChannel = tunnelTokenSessionTokenLocalTunnelChannels.remove(key);
        if (localTunnelChannel != null) {
            localTunnelChannel.close();
        }
    }



    public void getLocalTunnelChannel(
            @NotNull final String localAddr,
            final int localPort,
            final long tunnelToken,
            final long sessionToken,
            final Channel tunnelClientChannel,
            @NotNull final GetLocalTunnelChannelCallback callback) {
        logger.info("localAddr: {}, localPort: {},tunnelToken:{}, sessionToken: {}", localAddr, localAddr, tunnelToken, sessionToken);
        logger.info("tunnelTokenSessionTokenLocalTunnelChannels: {}", tunnelTokenSessionTokenLocalTunnelChannels);
        final String key = String.format("%d@%d", tunnelToken, sessionToken);
        Channel localTunnelChannel = tunnelTokenSessionTokenLocalTunnelChannels.get(key);
        if (localTunnelChannel != null && localTunnelChannel.isActive()) {
            callback.success(localTunnelChannel);
        } else {
            bootstrap.connect(localAddr, localPort).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        // 二次检查是否有可用的Channel缓存
                        Channel localTunnelChannel = tunnelTokenSessionTokenLocalTunnelChannels.get(key);
                        if (localTunnelChannel == null || !localTunnelChannel.isActive()) {
                            removeLocalTunnelChannel(tunnelToken, sessionToken);
                            localTunnelChannel = future.channel();
                            localTunnelChannel.attr(ATTR_TUNNEL_TOKEN).set(tunnelToken);
                            localTunnelChannel.attr(ATTR_SESSION_TOKEN).set(sessionToken);
                            localTunnelChannel.attr(ATTR_NEXT_CHANNEL).set(tunnelClientChannel);
                            tunnelTokenSessionTokenLocalTunnelChannels.put(key, localTunnelChannel);
                        } else {
                            future.channel().close();
                        }
                        callback.success(localTunnelChannel);
                    } else {
                        callback.error(future.cause());
                    }
                }
            });
        }
    }

    public interface GetLocalTunnelChannelCallback {
        void success(@NotNull Channel localTunnelChannel);

        void error(Throwable cause);
    }

}
