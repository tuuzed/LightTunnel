package lighttunnel.internal.server.traffic

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import lighttunnel.internal.server.util.AK_SESSION_CHANNELS
import lighttunnel.listener.OnTrafficListener

internal class TrafficHandler(
    private val onTrafficListener: OnTrafficListener?
) : ChannelDuplexHandler() {

    @Throws(Exception::class)
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is ByteBuf) {
            ctx.channel().attr(AK_SESSION_CHANNELS).get()?.apply {
                val bytes = msg.readableBytes()
                statistics.incInboundBytes(bytes)
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
                statistics.incOutboundBytes(bytes)
                onTrafficListener?.onOutbound(tunnelRequest, bytes)
            }
        }
        super.write(ctx, msg, promise)
    }

}