package lighttunnel.server.local

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import lighttunnel.base.proto.ProtoMsg
import lighttunnel.base.proto.emptyBytes
import lighttunnel.base.utils.loggerDelegate
import lighttunnel.server.utils.AK_NEXT_CHANNEL
import lighttunnel.server.utils.AK_SESSION_ID
import lighttunnel.server.utils.AK_TUNNEL_ID

internal class LocalTcpClientChannelHandler(
    private val localTcpClient: LocalTcpClient
) : SimpleChannelInboundHandler<ByteBuf>() {

    private val logger by loggerDelegate()

    @Throws(Exception::class)
    override fun channelActive(ctx: ChannelHandlerContext) {
        super.channelActive(ctx)
        val tunnelId = ctx.channel().attr(AK_TUNNEL_ID).get()
        val sessionId = ctx.channel().attr(AK_SESSION_ID).get()
        val nextChannel = ctx.channel().attr(AK_NEXT_CHANNEL).get()
        if (tunnelId != null && sessionId != null && nextChannel != null) {
            nextChannel.writeAndFlush(ProtoMsg.LOCAL_CONNECTED(tunnelId, sessionId))
        }
    }

    @Throws(Exception::class)
    override fun channelInactive(ctx: ChannelHandlerContext) {
        logger.trace("channelInactive: {}", ctx)
        val tunnelId = ctx.channel().attr(AK_TUNNEL_ID).get()
        val sessionId = ctx.channel().attr(AK_SESSION_ID).get()
        if (tunnelId != null && sessionId != null) {
            localTcpClient.removeLocalChannel(tunnelId, sessionId)
                ?.writeAndFlush(Unpooled.EMPTY_BUFFER)
                ?.addListener(ChannelFutureListener.CLOSE)
            ctx.channel().attr(AK_NEXT_CHANNEL).get()
                ?.writeAndFlush(ProtoMsg.LOCAL_DISCONNECT(tunnelId, sessionId))
        }
        super.channelInactive(ctx)
    }

    @Throws(Exception::class)
    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
        logger.trace("exceptionCaught: {}", ctx, cause)
        ctx ?: return
        ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
    }

    @Throws(Exception::class)
    override fun channelRead0(ctx: ChannelHandlerContext?, msg: ByteBuf?) {
        logger.trace("channelRead0: {}", ctx)
        ctx ?: return
        msg ?: return
        val tunnelId = ctx.channel().attr(AK_TUNNEL_ID).get()
        val sessionId = ctx.channel().attr(AK_SESSION_ID).get()
        val nextChannel = ctx.channel().attr(AK_NEXT_CHANNEL).get()
        if (tunnelId != null && sessionId != null && nextChannel != null) {
            val data = ByteBufUtil.getBytes(msg) ?: emptyBytes
            nextChannel.writeAndFlush(ProtoMsg.TRANSFER(tunnelId, sessionId, data))
        }
    }

}
