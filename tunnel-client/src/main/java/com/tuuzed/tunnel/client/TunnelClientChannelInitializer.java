package com.tuuzed.tunnel.client;

import com.tuuzed.tunnel.client.local.LocalConnect;
import com.tuuzed.tunnel.proto.ProtoHeartbeatHandler;
import com.tuuzed.tunnel.proto.ProtoMessageDecoder;
import com.tuuzed.tunnel.proto.ProtoMessageEncoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TunnelClientChannelInitializer extends ChannelInitializer<SocketChannel> {
    @Nullable
    private final SslContext sslContext;
    @NotNull
    private final LocalConnect localConnect;
    @NotNull
    private final TunnelClientChannelHandler.ChannelListener channelListener;

    public TunnelClientChannelInitializer(
        @Nullable SslContext sslContext,
        @NotNull LocalConnect localConnect,
        @NotNull TunnelClientChannelHandler.ChannelListener listener
    ) {
        this.sslContext = sslContext;
        this.localConnect = localConnect;
        this.channelListener = listener;
    }


    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        if (sslContext != null) {
            ch.pipeline().addFirst(
                new SslHandler(sslContext.newEngine(ch.alloc()))
            );
        }
        ch.pipeline()
            .addLast(new ProtoMessageDecoder())
            .addLast(new ProtoMessageEncoder())
            .addLast(new ProtoHeartbeatHandler())
            .addLast(new TunnelClientChannelHandler(
                localConnect, channelListener
            ))
        ;
    }
}
