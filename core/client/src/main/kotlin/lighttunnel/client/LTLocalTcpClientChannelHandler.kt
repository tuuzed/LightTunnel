package lighttunnel.client

import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import lighttunnel.logging.logger
import lighttunnel.proto.LTCommand
import lighttunnel.proto.LTMassage
import lighttunnel.util.long2Bytes
import lighttunnel.util.toBytes

class LTLocalTcpClientChannelHandler(
    private val localTcpClient: LTLocalTcpClient
) : SimpleChannelInboundHandler<ByteBuf>() {
    private val logger by logger()

    @Throws(Exception::class)
    override fun channelInactive(ctx: ChannelHandlerContext) {
        logger.trace("channelInactive: {}", ctx)
        val tunnelId = ctx.channel().attr<Long>(AK_TUNNEL_ID).get()
        val sessionId = ctx.channel().attr<Long>(AK_SESSION_ID).get()
        if (tunnelId != null && sessionId != null) {
            localTcpClient.removeLocalChannel(tunnelId, sessionId)?.close()
            val nextChannel = ctx.channel().attr<Channel>(AK_NEXT_CHANNEL).get()
            if (nextChannel != null) {
                val head = ctx.alloc().long2Bytes(tunnelId, sessionId)
                nextChannel.writeAndFlush(LTMassage(LTCommand.LOCAL_DISCONNECT, head))
            }
        }
        super.channelInactive(ctx)
    }

    @Throws(Exception::class)
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        logger.trace("exceptionCaught: {}", ctx, cause)
        ctx.close()
    }

    @Throws(Exception::class)
    override fun channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf) {
        val tunnelId = ctx.channel().attr<Long>(AK_TUNNEL_ID).get()
        val sessionId = ctx.channel().attr<Long>(AK_SESSION_ID).get()
        val nextChannel = ctx.channel().attr<Channel>(AK_NEXT_CHANNEL).get()
        if (tunnelId != null && sessionId != null && nextChannel != null) {
            val head = ctx.alloc().long2Bytes(tunnelId, sessionId)
            val data = msg.toBytes()
            nextChannel.writeAndFlush(LTMassage(LTCommand.TRANSFER, head, data))
        }
    }

}