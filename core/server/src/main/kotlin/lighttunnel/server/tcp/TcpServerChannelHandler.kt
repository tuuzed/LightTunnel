package lighttunnel.server.tcp

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import lighttunnel.logger.loggerDelegate
import lighttunnel.proto.ProtoMessage
import lighttunnel.proto.ProtoMessageType
import lighttunnel.server.util.AttributeKeys
import lighttunnel.util.LongUtil
import java.net.InetSocketAddress

class TcpServerChannelHandler(
    private val registry: TcpRegistry
) : SimpleChannelInboundHandler<ByteBuf>() {
    private val logger by loggerDelegate()

    override fun channelActive(ctx: ChannelHandlerContext?) {
        logger.trace("channelActive: {}", ctx)
        if (ctx != null) {
            val tcpFd = ctx.tcpFd
            if (tcpFd != null) {
                var sessionId = ctx.channel().attr(AttributeKeys.AK_SESSION_ID).get()
                if (sessionId == null) {
                    sessionId = tcpFd.sessionChannels.putChannel(ctx.channel())
                    ctx.channel().attr(AttributeKeys.AK_SESSION_ID).set(sessionId)
                }
                val head = LongUtil.toBytes(tcpFd.tunnelId, sessionId)
                tcpFd.tunnelChannel.writeAndFlush(ProtoMessage(ProtoMessageType.REMOTE_CONNECTED, head))
            } else {
                ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
            }
        }
        super.channelActive(ctx)
    }

    override fun channelInactive(ctx: ChannelHandlerContext?) {
        logger.trace("channelInactive: {}", ctx)
        if (ctx != null) {
            val tcpFd = ctx.tcpFd
            if (tcpFd != null) {
                val sessionId = ctx.channel().attr(AttributeKeys.AK_SESSION_ID).get()
                if (sessionId != null) {
                    tcpFd.sessionChannels.removeChannel(sessionId)?.also {
                        it.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
                    }
                }
                // 解决 HTTP/1.x 数据传输问题
                ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener {
                    val head = LongUtil.toBytes(tcpFd.tunnelId, sessionId)
                    tcpFd.tunnelChannel.writeAndFlush(
                        ProtoMessage(ProtoMessageType.REMOTE_DISCONNECT, head)
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
        val sessionId = ctx.channel().attr(AttributeKeys.AK_SESSION_ID).get() ?: return
        val fd = ctx.tcpFd ?: return
        val head = LongUtil.toBytes(fd.tunnelId, sessionId)
        val data = ByteBufUtil.getBytes(msg)
        fd.tunnelChannel.writeAndFlush(ProtoMessage(ProtoMessageType.TRANSFER, head, data))
    }

    private val ChannelHandlerContext?.tcpFd: TcpFd?
        get() {
            this ?: return null
            val sa = this.channel().localAddress()
            return if (sa is InetSocketAddress) registry.getTcpFd(sa.port) else null
        }

}