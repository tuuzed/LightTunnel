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


public class LocalTunnel {

    private static final Logger logger = LoggerFactory.getLogger(LocalTunnel.class);

    private static class InstanceHolder {
        private static final LocalTunnel instance = new LocalTunnel();
    }

    @NotNull
    public static LocalTunnel getInstance() {
        return InstanceHolder.instance;
    }

    private final Map<String, Channel> channels = new ConcurrentHashMap<>();

    private final Bootstrap bootstrap;

    private LocalTunnel() {
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast(new LocalTunnelHandler())
                        ;
                    }
                });
    }

    public void getLocalTunnelChannel(
            @NotNull final String localAddr,
            final int localPort,
            final long tunnelToken,
            final long sessionToken,
            @NotNull final GetLocalTunnelChannelCallback callback) {
        final String channelKey = localAddr + ":" + localPort + "#" + tunnelToken + "," + sessionToken;
        Channel channel = channels.get(channelKey);
        if (channel != null && channel.isActive()) {
            callback.success(channel);
        } else {
            channels.remove(channelKey);
            bootstrap.connect(localAddr, localPort).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        Channel channel = future.channel();
                        channels.put(channelKey, channel);
                        callback.success(channel);
                    } else {
                        callback.error(future.cause());
                    }
                }
            });
        }
    }

    public interface GetLocalTunnelChannelCallback {
        void success(@NotNull Channel channel);

        void error(Throwable cause);
    }

}
