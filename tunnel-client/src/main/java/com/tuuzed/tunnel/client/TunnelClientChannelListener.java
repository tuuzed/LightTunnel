package com.tuuzed.tunnel.client;

import io.netty.channel.ChannelHandlerContext;

public interface TunnelClientChannelListener {
    void channelInactive(ChannelHandlerContext ctx) throws Exception;
}
