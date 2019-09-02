package tunnel2.server.tcp

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import tunnel2.common.logging.LoggerFactory
import tunnel2.common.proto.ProtoCw
import tunnel2.common.proto.ProtoMessage
import tunnel2.server.internal.AK_SESSION_ID
import java.net.InetSocketAddress

class TcpServerChannelHandler(
    private val registry: TcpRegistry
) : SimpleChannelInboundHandler<ByteBuf>() {

    companion object {
        private val logger = LoggerFactory.getLogger(TcpServerChannelHandler::class.java)
    }

    override fun channelActive(ctx: ChannelHandlerContext?) {
        if (ctx != null) {
            val sa = ctx.channel().localAddress() as InetSocketAddress
            registry.getDescriptorByPort(sa.port)?.also {
                val tunnelChannel = it.sessionChannels.tunnelChannel
                var sessionId = ctx.channel().attr<Long>(AK_SESSION_ID).get()
                if (sessionId == null) {
                    sessionId = it.sessionChannels.putSessionChannel(ctx.channel())
                    ctx.channel().attr<Long>(AK_SESSION_ID).set(sessionId)
                }
                tunnelChannel.writeAndFlush(
                    ProtoMessage(
                        ProtoCw.REMOTE_CONNECTED,
                        it.sessionChannels.tunnelId,
                        sessionId
                    )
                )
            } ?: ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
        }
        super.channelActive(ctx)
    }

    override fun channelInactive(ctx: ChannelHandlerContext?) {
        if (ctx != null) {
            val sa = ctx.channel().localAddress() as InetSocketAddress
            registry.getDescriptorByPort(sa.port)?.also {
                val tunnelChannel = it.sessionChannels.tunnelChannel
                val sessionId = ctx.channel().attr<Long>(AK_SESSION_ID).get()
                if (sessionId != null) {
                    val channel = it.sessionChannels.removeSessionChannel(sessionId)
                    channel?.writeAndFlush(Unpooled.EMPTY_BUFFER)?.addListener(ChannelFutureListener.CLOSE)
                }
                // 解决 HTTP/1.x 数据传输问题
                ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener { _ ->
                    tunnelChannel.writeAndFlush(
                        ProtoMessage(
                            ProtoCw.REMOTE_DISCONNECT,
                            it.sessionChannels.tunnelId,
                            sessionId
                        )
                    )
                }
            }
            ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
        }
        super.channelInactive(ctx)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
        logger.trace("exceptionCaught: {}", ctx, cause)
        ctx?.channel()?.writeAndFlush(Unpooled.EMPTY_BUFFER)?.addListener(ChannelFutureListener.CLOSE)
    }

    override fun channelRead0(ctx: ChannelHandlerContext?, msg: ByteBuf?) {
        logger.trace("channelRead0: {}", ctx)
        ctx ?: return
        msg ?: return
        val sa = ctx.channel().localAddress() as InetSocketAddress
        val sessionId = ctx.channel().attr<Long>(AK_SESSION_ID).get() ?: return
        val descriptor = registry.getDescriptorByPort(sa.port) ?: return
        val data = ByteArray(msg.readableBytes())
        msg.readBytes(data)
        descriptor.sessionChannels.tunnelChannel.writeAndFlush(
            ProtoMessage(
                ProtoCw.TRANSFER,
                descriptor.sessionChannels.tunnelId,
                sessionId,
                data
            )
        )
    }

}