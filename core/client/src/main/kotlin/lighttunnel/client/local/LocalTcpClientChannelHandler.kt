package lighttunnel.client.local

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import lighttunnel.client.util.AttributeKeys
import lighttunnel.logger.loggerDelegate
import lighttunnel.proto.ProtoMessage
import lighttunnel.proto.ProtoMessageType
import lighttunnel.util.LongUtil

class LocalTcpClientChannelHandler(
    private val localTcpClient: LocalTcpClient
) : SimpleChannelInboundHandler<ByteBuf>() {

    private val logger by loggerDelegate()

    @Throws(Exception::class)
    override fun channelInactive(ctx: ChannelHandlerContext) {
        logger.trace("channelInactive: {}", ctx)
        val tunnelId = ctx.channel().attr(AttributeKeys.AK_TUNNEL_ID).get()
        val sessionId = ctx.channel().attr(AttributeKeys.AK_SESSION_ID).get()
        if (tunnelId != null && sessionId != null) {
            localTcpClient.removeLocalChannel(tunnelId, sessionId)
                ?.writeAndFlush(Unpooled.EMPTY_BUFFER)
                ?.addListener(ChannelFutureListener.CLOSE)
            val nextChannel = ctx.channel().attr(AttributeKeys.AK_NEXT_CHANNEL).get()
            if (nextChannel != null) {
                val head = LongUtil.toBytes(tunnelId, sessionId)
                nextChannel.writeAndFlush(ProtoMessage(ProtoMessageType.LOCAL_DISCONNECT, head))
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
    override fun channelRead0(ctx: ChannelHandlerContext?, msg: ByteBuf?) {
        logger.trace("channelRead0: {}", ctx)
        ctx ?: return
        msg ?: return
        val tunnelId = ctx.channel().attr(AttributeKeys.AK_TUNNEL_ID).get()
        val sessionId = ctx.channel().attr(AttributeKeys.AK_SESSION_ID).get()
        val nextChannel = ctx.channel().attr(AttributeKeys.AK_NEXT_CHANNEL).get()
        if (tunnelId != null && sessionId != null && nextChannel != null) {
            val head = LongUtil.toBytes(tunnelId, sessionId)
            val data = ByteBufUtil.getBytes(msg)
            nextChannel.writeAndFlush(ProtoMessage(ProtoMessageType.TRANSFER, head, data))
        }
    }

}