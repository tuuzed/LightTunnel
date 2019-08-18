package com.tuuzed.tunnel.client.local;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import org.jetbrains.annotations.NotNull;

public class LocalConnectChannelInitializer extends ChannelInitializer<SocketChannel> {
    @NotNull
    private final LocalConnect localConnect;

    public LocalConnectChannelInitializer(@NotNull LocalConnect localConnect) {
        this.localConnect = localConnect;
    }


    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline()
            .addLast(new LocalConnectChannelHandler(localConnect))
        ;
    }
}
