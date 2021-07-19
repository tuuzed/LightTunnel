package lighttunnel.server

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import lighttunnel.base.TunnelRequest
import lighttunnel.base.TunnelType
import lighttunnel.base.proto.*
import lighttunnel.base.utils.IncIds
import lighttunnel.base.utils.loggerDelegate
import lighttunnel.server.http.HttpFd
import lighttunnel.server.http.HttpTunnel
import lighttunnel.server.tcp.TcpFd
import lighttunnel.server.tcp.TcpTunnel
import lighttunnel.server.utils.AK_SESSION_CHANNELS
import lighttunnel.server.utils.SessionChannels

internal class TunnelServerDaemonChannelHandler(
    private val tunnelRequestInterceptor: TunnelRequestInterceptor?,
    private val tunnelIds: IncIds,
    private val tcpTunnel: TcpTunnel?,
    private val httpTunnel: HttpTunnel?,
    private val httpsTunnel: HttpTunnel?,
    private val callback: Callback
) : SimpleChannelInboundHandler<ProtoMsg>() {

    private val logger by loggerDelegate()

    @Throws(Exception::class)
    override fun channelInactive(ctx: ChannelHandlerContext) {
        logger.trace("channelInactive: {}", ctx)
        ctx.channel().attr(AK_SESSION_CHANNELS).get()?.also { sc ->
            when (sc.tunnelRequest.tunnelType) {
                TunnelType.TCP -> callback.onChannelInactive(ctx, tcpTunnel?.stopTunnel(sc.tunnelRequest.remotePort))
                TunnelType.HTTP -> callback.onChannelInactive(ctx, httpTunnel?.stopTunnel(sc.tunnelRequest.host))
                TunnelType.HTTPS -> callback.onChannelInactive(ctx, httpsTunnel?.stopTunnel(sc.tunnelRequest.host))
                else -> {
                    // Nothing
                }
            }
        }
        ctx.channel().attr(AK_SESSION_CHANNELS).set(null)
        super.channelInactive(ctx)
    }

    @Throws(Exception::class)
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable?) {
        logger.trace("exceptionCaught: {}", ctx, cause)
        ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
    }

    @Throws(Exception::class)
    override fun channelRead0(ctx: ChannelHandlerContext, msg: ProtoMsg) {
        logger.trace("channelRead0: {}", ctx)
        when (msg.type) {
            ProtoMsgType.HEARTBEAT_PING -> doHandlePingMessage(ctx, msg as HeartbeatPingMsg)
            ProtoMsgType.REQUEST -> doHandleRequestMessage(ctx, msg as RequestMsg)
            ProtoMsgType.TRANSFER -> doHandleTransferMessage(ctx, msg as TransferMsg)
            ProtoMsgType.LOCAL_CONNECTED -> doHandleLocalConnectedMessage(ctx, msg as LocalConnectedMsg)
            ProtoMsgType.LOCAL_DISCONNECT -> doHandleLocalDisconnectMessage(ctx, msg as LocalDisconnectMsg)
            ProtoMsgType.FORCE_OFF_REPLY -> doHandleForceOffReplyMessage(ctx, msg as ForceOffReplyMsg)
            else -> {
                // Nothing
            }
        }
    }

    @Throws(Exception::class)
    private fun doHandlePingMessage(ctx: ChannelHandlerContext, msg: HeartbeatPingMsg) {
        logger.trace("doHandlePingMessage# {}, {}", ctx, msg)
        ctx.writeAndFlush(ProtoMsg.HEARTBEAT_PONG())
    }

    @Throws(Exception::class)
    private fun doHandleRequestMessage(ctx: ChannelHandlerContext, msg: RequestMsg) {
        logger.trace("doHandleRequestMessage# {}, {}", ctx, msg)
        try {
            val originalTunnelRequest = msg.request
            val finalTunnelRequest = tunnelRequestInterceptor?.intercept(originalTunnelRequest)
                ?: originalTunnelRequest
            logger.trace("TunnelRequest=> original: {}, final: {}", originalTunnelRequest, finalTunnelRequest)
            when (finalTunnelRequest.tunnelType) {
                TunnelType.TCP -> {
                    val tcpTunnel = tcpTunnel ?: throw ProtoException("TCP协议隧道未开启")
                    tcpTunnel.handleRequestMessage(ctx, finalTunnelRequest)
                }
                TunnelType.HTTP -> {
                    val httpTunnel = httpTunnel ?: throw ProtoException("HTTP协议隧道未开启")
                    httpTunnel.handleRequestMessage(ctx, finalTunnelRequest)
                }
                TunnelType.HTTPS -> {
                    val httpsTunnel = httpsTunnel ?: throw ProtoException("HTTPS协议隧道未开启")
                    httpsTunnel.handleRequestMessage(ctx, finalTunnelRequest)
                }
                else -> throw ProtoException("不支持的隧道类型")
            }
        } catch (e: Exception) {
            ctx.channel().writeAndFlush(
                ProtoMsg.RESPONSE_ERR(e)
            ).addListener(ChannelFutureListener.CLOSE)
        }
    }

    @Throws(Exception::class)
    private fun doHandleTransferMessage(ctx: ChannelHandlerContext, msg: TransferMsg) {
        logger.trace("doHandleTransferMessage# {}, {}", ctx, msg)
        val sessionChannels = ctx.channel().attr(AK_SESSION_CHANNELS).get() ?: return
        val sessionChannel = sessionChannels.getChannel(msg.sessionId)
        sessionChannel?.writeAndFlush(Unpooled.wrappedBuffer(msg.data))
    }

    @Throws(Exception::class)
    private fun doHandleLocalConnectedMessage(ctx: ChannelHandlerContext, msg: LocalConnectedMsg) {
        logger.trace("doHandleLocalConnectedMessage# {}, {}", ctx, msg)
        // 无须处理
    }

    @Throws(Exception::class)
    private fun doHandleLocalDisconnectMessage(ctx: ChannelHandlerContext, msg: LocalDisconnectMsg) {
        logger.trace("doHandleLocalDisconnectMessage# {}, {}", ctx, msg)
        val sessionChannels = ctx.channel().attr(AK_SESSION_CHANNELS).get() ?: return
        val sessionChannel = sessionChannels.removeChannel(msg.sessionId)
        // 解决 HTTP/1.x 数据传输问题
        sessionChannel?.writeAndFlush(Unpooled.EMPTY_BUFFER)?.addListener(ChannelFutureListener.CLOSE)
    }

    @Throws(Exception::class)
    private fun doHandleForceOffReplyMessage(ctx: ChannelHandlerContext, msg: ForceOffReplyMsg) {
        logger.trace("doHandleForceOffReplyMessage# {}, {}", ctx, msg)
        ctx.channel()?.close()
    }

    @Throws(Exception::class)
    private fun TcpTunnel.handleRequestMessage(ctx: ChannelHandlerContext, tunnelRequest: TunnelRequest) {
        requireNotRegistered(tunnelRequest.remotePort)
        val tunnelId = tunnelIds.nextId
        val sessionChannels = SessionChannels(tunnelId, tunnelRequest, ctx.channel())
        ctx.channel().attr(AK_SESSION_CHANNELS).set(sessionChannels)
        val fd = startTunnel(null, tunnelRequest.remotePort, sessionChannels)
        callback.onChannelConnected(ctx, fd)
        ctx.channel().writeAndFlush(ProtoMsg.RESPONSE_OK(tunnelId, tunnelRequest))
    }

    @Throws(Exception::class)
    private fun HttpTunnel.handleRequestMessage(ctx: ChannelHandlerContext, tunnelRequest: TunnelRequest) {
        requireNotRegistered(tunnelRequest.host)
        val tunnelId = tunnelIds.nextId
        val sessionChannels = SessionChannels(tunnelId, tunnelRequest, ctx.channel())
        ctx.channel().attr(AK_SESSION_CHANNELS).set(sessionChannels)
        val fd = startTunnel(tunnelRequest.host, sessionChannels)
        callback.onChannelConnected(ctx, fd)
        ctx.channel().writeAndFlush(ProtoMsg.RESPONSE_OK(tunnelId, tunnelRequest))
    }

    internal interface Callback {
        fun onChannelInactive(ctx: ChannelHandlerContext, tcpFd: TcpFd?)
        fun onChannelInactive(ctx: ChannelHandlerContext, httpFd: HttpFd?)
        fun onChannelConnected(ctx: ChannelHandlerContext, tcpFd: TcpFd?)
        fun onChannelConnected(ctx: ChannelHandlerContext, httpFd: HttpFd?)
    }

}
