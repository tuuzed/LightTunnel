package lighttunnel.server.tcp

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import lighttunnel.base.entity.RemoteConnection
import lighttunnel.base.proto.ProtoMsgRemoteConnected
import lighttunnel.base.proto.ProtoMsgRemoteDisconnect
import lighttunnel.base.proto.ProtoMsgTransfer
import lighttunnel.base.utils.loggerDelegate
import lighttunnel.server.utils.AK_SESSION_ID
import java.net.InetSocketAddress

internal class TcpTunnelChannelHandler(
    private val registry: TcpRegistry
) : SimpleChannelInboundHandler<ByteBuf>() {
    private val logger by loggerDelegate()

    override fun channelActive(ctx: ChannelHandlerContext) {
        logger.trace("channelActive: {}", ctx)
        val tcpFd = ctx.tcpFd
        if (tcpFd != null) {
            var sessionId = ctx.channel().attr(AK_SESSION_ID).get()
            if (sessionId == null) {
                sessionId = tcpFd.putChannel(ctx.channel())
                ctx.channel().attr(AK_SESSION_ID).set(sessionId)
            }
            tcpFd.tunnelChannel.writeAndFlush(
                ProtoMsgRemoteConnected(
                    tcpFd.tunnelId,
                    sessionId,
                    RemoteConnection(ctx.channel().remoteAddress()).toJsonString() ?: ""
                )
            )
        } else {
            ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
        }
        super.channelActive(ctx)
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        logger.trace("channelInactive: {}", ctx)
        val tcpFd = ctx.tcpFd
        if (tcpFd != null) {
            val sessionId = ctx.channel().attr(AK_SESSION_ID).get()
            if (sessionId != null) {
                val sessionChannel = tcpFd.removeChannel(sessionId)
                // 解决 HTTP/1.x 数据传输问题
                sessionChannel?.writeAndFlush(Unpooled.EMPTY_BUFFER)?.addListener(ChannelFutureListener.CLOSE)
            }
            ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener {
                tcpFd.tunnelChannel.writeAndFlush(
                    ProtoMsgRemoteDisconnect(
                        tcpFd.tunnelId,
                        sessionId ?: 0,
                        RemoteConnection(ctx.channel().remoteAddress()).toJsonString() ?: ""
                    )
                )
            }
        }
        ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
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
        val data = ByteBufUtil.getBytes(msg)
        tcpFd.tunnelChannel.writeAndFlush(ProtoMsgTransfer(tcpFd.tunnelId, sessionId, data))
    }

    private val ChannelHandlerContext?.tcpFd: DefaultTcpFd?
        get() {
            this ?: return null
            val sa = this.channel().localAddress()
            return if (sa is InetSocketAddress) registry.getTcpFd(sa.port) else null
        }

}
