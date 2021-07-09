package lighttunnel.server.traffic

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import lighttunnel.server.listener.OnTrafficListener
import lighttunnel.server.utils.AK_SESSION_CHANNELS

internal class TrafficHandler(
    private val onTrafficListener: OnTrafficListener?
) : ChannelDuplexHandler() {

    @Throws(Exception::class)
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is ByteBuf) {
            ctx.channel().attr(AK_SESSION_CHANNELS).get()?.apply {
                val bytes = msg.readableBytes()
                trafficStats.incInboundBytes(bytes)
                onTrafficListener?.onInbound(tunnelRequest, bytes)
            }
        }
        ctx.fireChannelRead(msg)
    }

    @Throws(Exception::class)
    override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise?) {
        if (msg is ByteBuf) {
            ctx.channel().attr(AK_SESSION_CHANNELS).get()?.apply {
                val bytes = msg.readableBytes()
                trafficStats.incOutboundBytes(bytes)
                onTrafficListener?.onOutbound(tunnelRequest, bytes)
            }
        }
        super.write(ctx, msg, promise)
    }

}
