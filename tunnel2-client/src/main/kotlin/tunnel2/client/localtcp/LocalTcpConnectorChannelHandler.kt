package tunnel2.client.localtcp

import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import tunnel2.client.internal.AK_NEXT_CHANNEL
import tunnel2.client.internal.AK_SESSION_ID
import tunnel2.client.internal.AK_TUNNEL_ID
import tunnel2.common.logging.LoggerFactory
import tunnel2.common.proto.ProtoCw
import tunnel2.common.proto.ProtoMessage

class LocalTcpConnectorChannelHandler(
    private val localTcpConnector: LocalTcpConnector
) : SimpleChannelInboundHandler<ByteBuf>() {

    companion object {
        private val logger = LoggerFactory.getLogger(LocalTcpConnectorChannelHandler::class.java)
    }

    @Throws(Exception::class)
    override fun channelInactive(ctx: ChannelHandlerContext) {
        logger.trace("channelInactive: {}", ctx)
        val tunnelId = ctx.channel().attr<Long>(AK_TUNNEL_ID).get()
        val sessionId = ctx.channel().attr<Long>(AK_SESSION_ID).get()
        if (tunnelId != null && sessionId != null) {
            localTcpConnector.removeLocalChannel(tunnelId, sessionId)?.close()
            ctx.channel().attr<Channel>(AK_NEXT_CHANNEL).get()?.writeAndFlush(
                ProtoMessage(
                    ProtoCw.LOCAL_DISCONNECT,
                    tunnelId,
                    sessionId
                )
            )
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
        if (tunnelId != null && sessionId != null) {
            val data = ByteArray(msg.readableBytes())
            msg.readBytes(data)
            ctx.channel().attr<Channel>(AK_NEXT_CHANNEL).get().writeAndFlush(
                ProtoMessage(
                    ProtoCw.TRANSFER,
                    tunnelId,
                    sessionId,
                    data
                )
            )
        }

    }
}