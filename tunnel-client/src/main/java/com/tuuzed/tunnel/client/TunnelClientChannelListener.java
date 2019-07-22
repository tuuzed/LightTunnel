package com.tuuzed.tunnel.client;

import io.netty.channel.ChannelHandlerContext;
import org.jetbrains.annotations.NotNull;

interface TunnelClientChannelListener {
    void channelInactive(@NotNull ChannelHandlerContext ctx) throws Exception;

    void tunnelConnected(@NotNull ChannelHandlerContext ctx);

}
