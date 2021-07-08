package lighttunnel.base.heartbeat

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.timeout.IdleStateEvent
import io.netty.handler.timeout.IdleStateHandler
import lighttunnel.base.proto.ProtoMsg
import lighttunnel.base.utils.loggerDelegate
import java.util.concurrent.TimeUnit

/**
 * 心跳处理器
 */
class HeartbeatHandler(
    readerIdleTime: Long,
    writerIdleTime: Long,
    observeOutput: Boolean = false,
    allIdleTime: Long = 0L,
    unit: TimeUnit = TimeUnit.SECONDS
) : IdleStateHandler(observeOutput, readerIdleTime, writerIdleTime, allIdleTime, unit) {

    private val logger by loggerDelegate()

    @Throws(Exception::class)
    override fun channelIdle(ctx: ChannelHandlerContext?, evt: IdleStateEvent?) {
        logger.trace("channelIdle: {}, {}", ctx, evt)
        ctx?.channel()?.writeAndFlush(ProtoMsg.HEARTBEAT_PING())
        super.channelIdle(ctx, evt)
    }

}
