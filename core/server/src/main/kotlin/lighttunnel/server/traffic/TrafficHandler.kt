package lighttunnel.server.traffic

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import lighttunnel.server.util.AK_SESSION_CHANNELS


internal class TrafficHandler : ChannelDuplexHandler() {

    @Throws(Exception::class)
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is ByteBuf) {
            ctx.channel().attr(AK_SESSION_CHANNELS).get()?.apply {
                inboundBytes.getAndAdd(msg.readableBytes().toLong())
                updateAt.time = System.currentTimeMillis()
            }
        }
        ctx.fireChannelRead(msg)
    }

    @Throws(Exception::class)
    override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise?) {
        if (msg is ByteBuf) {
            ctx.channel().attr(AK_SESSION_CHANNELS).get()?.apply {
                outboundBytes.getAndAdd(msg.readableBytes().toLong())
                updateAt.time = System.currentTimeMillis()
            }
        }
        super.write(ctx, msg, promise)
    }

}