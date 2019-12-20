package lighttunnel.client.local

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import lighttunnel.client.util.AttrKeys
import lighttunnel.logging.loggerDelegate
import lighttunnel.proto.ProtoCommand
import lighttunnel.proto.ProtoMassage
import lighttunnel.util.long2Bytes
import lighttunnel.util.toBytes

class LocalTcpClientChannelHandler(
    private val localTcpClient: LocalTcpClient
) : SimpleChannelInboundHandler<ByteBuf>() {
    private val logger by loggerDelegate()

    @Throws(Exception::class)
    override fun channelInactive(ctx: ChannelHandlerContext) {
        logger.trace("channelInactive: {}", ctx)
        val tunnelId = ctx.channel().attr(AttrKeys.AK_TUNNEL_ID).get()
        val sessionId = ctx.channel().attr(AttrKeys.AK_SESSION_ID).get()
        if (tunnelId != null && sessionId != null) {
            localTcpClient.removeLocalChannel(tunnelId, sessionId)?.close()
            val nextChannel = ctx.channel().attr(AttrKeys.AK_NEXT_CHANNEL).get()
            if (nextChannel != null) {
                val head = ctx.alloc().long2Bytes(tunnelId, sessionId)
                nextChannel.writeAndFlush(ProtoMassage(ProtoCommand.LOCAL_DISCONNECT, head))
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
        val tunnelId = ctx.channel().attr(AttrKeys.AK_TUNNEL_ID).get()
        val sessionId = ctx.channel().attr(AttrKeys.AK_SESSION_ID).get()
        val nextChannel = ctx.channel().attr(AttrKeys.AK_NEXT_CHANNEL).get()
        if (tunnelId != null && sessionId != null && nextChannel != null) {
            val head = ctx.alloc().long2Bytes(tunnelId, sessionId)
            val data = msg.toBytes()
            nextChannel.writeAndFlush(ProtoMassage(ProtoCommand.TRANSFER, head, data))
        }
    }

}