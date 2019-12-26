package lighttunnel.client.callback

import io.netty.channel.ChannelHandlerContext

interface OnTunnelStateCallback {
    fun onTunnelInactive(ctx: ChannelHandlerContext) {}
    fun onTunnelConnected(ctx: ChannelHandlerContext) {}
}