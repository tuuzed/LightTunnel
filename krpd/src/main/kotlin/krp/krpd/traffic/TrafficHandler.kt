package krp.krpd.traffic

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import krp.common.entity.TunnelRequest
import krp.krpd.utils.AK_SESSION_CHANNELS

internal class TrafficHandler(
    private val callback: Callback?
) : ChannelDuplexHandler() {

    @Throws(Exception::class)
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is ByteBuf) {
            ctx.channel().attr(AK_SESSION_CHANNELS).get()?.apply {
                val bytes = msg.readableBytes()
                trafficStats.addInboundBytes(bytes)
                callback?.onInbound(tunnelRequest, bytes)
            }
        }
        ctx.fireChannelRead(msg)
    }

    @Throws(Exception::class)
    override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise?) {
        if (msg is ByteBuf) {
            ctx.channel().attr(AK_SESSION_CHANNELS).get()?.apply {
                val bytes = msg.readableBytes()
                trafficStats.addOutboundBytes(bytes)
                callback?.onOutbound(tunnelRequest, bytes)
            }
        }
        super.write(ctx, msg, promise)
    }

    internal interface Callback {
        fun onInbound(tunnelRequest: TunnelRequest, bytes: Int) {}
        fun onOutbound(tunnelRequest: TunnelRequest, bytes: Int) {}
    }

}