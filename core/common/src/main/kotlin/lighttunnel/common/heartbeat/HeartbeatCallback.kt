package lighttunnel.common.heartbeat

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.timeout.IdleStateEvent

fun interface HeartbeatCallback {
    fun invoke(ctx: ChannelHandlerContext, evt: IdleStateEvent): Boolean
}
