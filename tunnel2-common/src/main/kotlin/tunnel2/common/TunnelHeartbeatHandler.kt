package tunnel2.common

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.timeout.IdleStateEvent
import io.netty.handler.timeout.IdleStateHandler
import org.slf4j.LoggerFactory
import tunnel2.common.proto.ProtoCw
import tunnel2.common.proto.ProtoMessage
import java.util.concurrent.TimeUnit

/**
 * 心跳处理器
 */
class TunnelHeartbeatHandler @JvmOverloads constructor(
    observeOutput: Boolean = false,
    readerIdleTime: Long = 60 * 5L,
    writerIdleTime: Long = 60 * 3L,
    allIdleTime: Long = 0L,
    unit: TimeUnit = TimeUnit.SECONDS
) : IdleStateHandler(observeOutput, readerIdleTime, writerIdleTime, allIdleTime, unit) {

    companion object {
        private val logger = LoggerFactory.getLogger(TunnelHeartbeatHandler::class.java)
    }

    @Throws(Exception::class)
    override fun channelIdle(ctx: ChannelHandlerContext, evt: IdleStateEvent) {
        when (evt) {
            IdleStateEvent.FIRST_WRITER_IDLE_STATE_EVENT -> {
                logger.trace("channel write timeout {}", ctx)
                ctx.channel().writeAndFlush(ProtoMessage(cw = ProtoCw.PING))
            }
            IdleStateEvent.FIRST_READER_IDLE_STATE_EVENT -> {
                logger.trace("channel read timeout {}", ctx)
                ctx.channel().close()
            }
        }
        super.channelIdle(ctx, evt)
    }

}
