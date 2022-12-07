package lighttunnel.client.local

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import lighttunnel.client.utils.AK_AES128_KEY
import lighttunnel.client.utils.AK_NEXT_CHANNEL
import lighttunnel.client.utils.AK_SESSION_ID
import lighttunnel.client.utils.AK_TUNNEL_ID
import lighttunnel.common.proto.msg.ProtoMsgLocalConnected
import lighttunnel.common.proto.msg.ProtoMsgLocalDisconnect
import lighttunnel.common.proto.msg.ProtoMsgTransfer
import lighttunnel.common.utils.emptyBytes
import lighttunnel.common.utils.injectLogger
import lighttunnel.common.utils.tryEncryptAES128
import lighttunnel.common.utils.tryGZip

internal class LocalTcpClientChannelHandler(
    private val localTcpClient: LocalTcpClient
) : SimpleChannelInboundHandler<ByteBuf>() {

    private val logger by injectLogger()

    @Throws(Exception::class)
    override fun channelActive(ctx: ChannelHandlerContext) {
        super.channelActive(ctx)
        val tunnelId = ctx.channel().attr(AK_TUNNEL_ID).get()
        val sessionId = ctx.channel().attr(AK_SESSION_ID).get()
        val nextChannel = ctx.channel().attr(AK_NEXT_CHANNEL).get()
        if (tunnelId != null && sessionId != null && nextChannel != null) {
            nextChannel.writeAndFlush(ProtoMsgLocalConnected(tunnelId, sessionId))
        }
    }

    @Throws(Exception::class)
    override fun channelInactive(ctx: ChannelHandlerContext) {
        logger.trace("channelInactive: {}", ctx)
        val tunnelId = ctx.channel().attr(AK_TUNNEL_ID).get()
        val sessionId = ctx.channel().attr(AK_SESSION_ID).get()
        if (tunnelId != null && sessionId != null) {
            localTcpClient.removeLocalChannel(tunnelId, sessionId)?.writeAndFlush(Unpooled.EMPTY_BUFFER)
                ?.addListener(ChannelFutureListener.CLOSE)
            ctx.channel().attr(AK_NEXT_CHANNEL).get()?.writeAndFlush(ProtoMsgLocalDisconnect(tunnelId, sessionId))
        }
        super.channelInactive(ctx)
    }

    @Throws(Exception::class)
    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
        ctx ?: return
        logger.trace("exceptionCaught: {}", ctx, cause)
        ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
    }

    @Throws(Exception::class)
    override fun channelRead0(ctx: ChannelHandlerContext?, msg: ByteBuf?) {
        ctx ?: return
        msg ?: return
        logger.trace("channelRead0: {}", ctx)
        val tunnelId = ctx.channel().attr(AK_TUNNEL_ID).get()
        val sessionId = ctx.channel().attr(AK_SESSION_ID).get()
        val nextChannel = ctx.channel().attr(AK_NEXT_CHANNEL).get()
        if (tunnelId != null && sessionId != null && nextChannel != null) {
            val aes128Key = nextChannel.attr(AK_AES128_KEY).get()
            val compressedAndData = (ByteBufUtil.getBytes(msg) ?: emptyBytes)
                .tryGZip()
                .let {
                    it.first to if (it.second.isNotEmpty() && aes128Key != null) it.second.tryEncryptAES128(aes128Key) else it.second
                }
            nextChannel.writeAndFlush(
                ProtoMsgTransfer(
                    tunnelId, sessionId, compressedAndData.second, aes128Key, compressedAndData.first
                )
            )
        }
    }

}
