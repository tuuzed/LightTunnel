package lighttunnel.server.tcp

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import lighttunnel.base.logger.loggerDelegate
import lighttunnel.base.proto.ProtoMessage
import lighttunnel.base.proto.ProtoMessageType
import lighttunnel.base.util.LongUtil
import lighttunnel.openapi.RemoteConnection
import lighttunnel.server.util.AK_SESSION_ID
import java.net.InetSocketAddress

internal class TcpTunnelChannelHandler(
    private val registry: TcpRegistry
) : SimpleChannelInboundHandler<ByteBuf>() {
    private val logger by loggerDelegate()

    override fun channelActive(ctx: ChannelHandlerContext?) {
        logger.trace("channelActive: {}", ctx)
        if (ctx != null) {
            val tcpFd = ctx.tcpFd
            if (tcpFd != null) {
                var sessionId = ctx.channel().attr(AK_SESSION_ID).get()
                if (sessionId == null) {
                    sessionId = tcpFd.putChannel(ctx.channel())
                    ctx.channel().attr(AK_SESSION_ID).set(sessionId)
                }
                val head = LongUtil.toBytes(tcpFd.tunnelId, sessionId)
                tcpFd.tunnelChannel.writeAndFlush(
                    ProtoMessage(ProtoMessageType.REMOTE_CONNECTED, head, RemoteConnection(ctx.channel().remoteAddress()).toBytes())
                )
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
                val sessionId = ctx.channel().attr(AK_SESSION_ID).get()
                if (sessionId != null) {
                    val sessionChannel = tcpFd.removeChannel(sessionId)
                    // 解决 HTTP/1.x 数据传输问题
                    sessionChannel?.writeAndFlush(Unpooled.EMPTY_BUFFER)?.addListener(ChannelFutureListener.CLOSE)
                }
                ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener {
                    val head = LongUtil.toBytes(tcpFd.tunnelId, sessionId ?: 0)
                    tcpFd.tunnelChannel.writeAndFlush(
                        ProtoMessage(ProtoMessageType.REMOTE_DISCONNECT, head, RemoteConnection(ctx.channel().remoteAddress()).toBytes())
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
        val sessionId = ctx.channel().attr(AK_SESSION_ID).get() ?: return
        val tcpFd = ctx.tcpFd ?: return
        val head = LongUtil.toBytes(tcpFd.tunnelId, sessionId)
        val data = ByteBufUtil.getBytes(msg)
        tcpFd.tunnelChannel.writeAndFlush(ProtoMessage(ProtoMessageType.TRANSFER, head, data))
    }

    private val ChannelHandlerContext?.tcpFd: TcpFdDefaultImpl?
        get() {
            this ?: return null
            val sa = this.channel().localAddress()
            return if (sa is InetSocketAddress) registry.getTcpFd(sa.port) else null
        }

}