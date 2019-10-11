package com.tuuzed.lighttunnel.client

import io.netty.channel.ChannelHandlerContext

interface OnConnectStateListener {
    fun onChannelInactive(ctx: ChannelHandlerContext) {}
    fun onTunnelConnected(ctx: ChannelHandlerContext) {}
}