package lighttunnel.internal.server

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import lighttunnel.ProtoException
import lighttunnel.TunnelRequest
import lighttunnel.TunnelRequestInterceptor
import lighttunnel.TunnelType
import lighttunnel.internal.base.proto.ProtoMessage
import lighttunnel.internal.base.proto.message.*
import lighttunnel.internal.base.util.IncIds
import lighttunnel.internal.base.util.loggerDelegate
import lighttunnel.internal.server.http.HttpFdDefaultImpl
import lighttunnel.internal.server.http.HttpTunnel
import lighttunnel.internal.server.tcp.TcpFdDefaultImpl
import lighttunnel.internal.server.tcp.TcpTunnel
import lighttunnel.internal.server.util.AK_SESSION_CHANNELS
import lighttunnel.internal.server.util.SessionChannels

internal class TunnelServerDaemonChannelHandler(
    private val tunnelRequestInterceptor: TunnelRequestInterceptor?,
    private val tunnelIds: IncIds,
    private val tcpTunnel: TcpTunnel?,
    private val httpTunnel: HttpTunnel?,
    private val httpsTunnel: HttpTunnel?,
    private val callback: Callback
) : SimpleChannelInboundHandler<ProtoMessage>() {

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
    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
        logger.trace("exceptionCaught: {}", ctx, cause)
        ctx ?: return
        ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
    }

    @Throws(Exception::class)
    override fun channelRead0(ctx: ChannelHandlerContext?, msg: ProtoMessage?) {
        logger.trace("channelRead0: {}", ctx)
        ctx ?: return
        msg ?: return
        when (msg.type) {
            ProtoMessage.Type.PING -> doHandlePingMessage(ctx, msg as PingMessage)
            ProtoMessage.Type.REQUEST -> doHandleRequestMessage(ctx, msg as RequestMessage)
            ProtoMessage.Type.TRANSFER -> doHandleTransferMessage(ctx, msg as TransferMessage)
            ProtoMessage.Type.LOCAL_CONNECTED -> doHandleLocalConnectedMessage(ctx, msg as LocalConnectedMessage)
            ProtoMessage.Type.LOCAL_DISCONNECT -> doHandleLocalDisconnectMessage(ctx, msg as LocalDisconnectMessage)
            ProtoMessage.Type.FORCE_OFF_REPLY -> doHandleForceOffReplyMessage(ctx, msg as ForceOffReplyMessage)
            else -> {
                // Nothing
            }
        }
    }

    @Throws(Exception::class)
    private fun doHandlePingMessage(ctx: ChannelHandlerContext, msg: PingMessage) {
        logger.trace("doHandlePingMessage# {}, {}", ctx, msg)
        ctx.writeAndFlush(ProtoMessage.PONG())
    }

    @Throws(Exception::class)
    private fun doHandleRequestMessage(ctx: ChannelHandlerContext, msg: RequestMessage) {
        logger.trace("doHandleRequestMessage# {}, {}", ctx, msg)
        try {
            val originalTunnelRequest = msg.request
            val finalTunnelRequest = tunnelRequestInterceptor?.intercept(originalTunnelRequest)
                ?: originalTunnelRequest
            logger.trace("TunnelRequest=> original: {}, final: {}", originalTunnelRequest, finalTunnelRequest)
            when (finalTunnelRequest.tunnelType) {
                TunnelType.TCP -> {
                    val tcpTunnel = tcpTunnel ?: throw ProtoException("TCP协议隧道未开启")
                    tcpTunnel.handleTcpRequestMessage(ctx, finalTunnelRequest)
                }
                TunnelType.HTTP -> {
                    val httpTunnel = httpTunnel ?: throw ProtoException("HTTP协议隧道未开启")
                    httpTunnel.handleHttpRequestMessage(ctx, finalTunnelRequest)
                }
                TunnelType.HTTPS -> {
                    val httpsTunnel = httpsTunnel ?: throw ProtoException("HTTPS协议隧道未开启")
                    httpsTunnel.handleHttpRequestMessage(ctx, finalTunnelRequest)
                }
                else -> throw ProtoException("不支持的隧道类型")
            }
        } catch (e: Exception) {
            ctx.channel().writeAndFlush(
                ProtoMessage.RESPONSE_ERR(e)
            ).addListener(ChannelFutureListener.CLOSE)
        }
    }

    @Throws(Exception::class)
    private fun doHandleTransferMessage(ctx: ChannelHandlerContext, msg: TransferMessage) {
        logger.trace("doHandleTransferMessage# {}, {}", ctx, msg)
        val sessionChannels = ctx.channel().attr(AK_SESSION_CHANNELS).get() ?: return
        val sessionChannel = sessionChannels.getChannel(msg.sessionId)
        sessionChannel?.writeAndFlush(Unpooled.wrappedBuffer(msg.data))
    }

    @Throws(Exception::class)
    private fun doHandleLocalConnectedMessage(ctx: ChannelHandlerContext, msg: LocalConnectedMessage) {
        logger.trace("doHandleLocalConnectedMessage# {}, {}", ctx, msg)
        // 无须处理
    }

    @Throws(Exception::class)
    private fun doHandleLocalDisconnectMessage(ctx: ChannelHandlerContext, msg: LocalDisconnectMessage) {
        logger.trace("doHandleLocalDisconnectMessage# {}, {}", ctx, msg)
        val sessionChannels = ctx.channel().attr(AK_SESSION_CHANNELS).get() ?: return
        val sessionChannel = sessionChannels.removeChannel(msg.sessionId)
        // 解决 HTTP/1.x 数据传输问题
        sessionChannel?.writeAndFlush(Unpooled.EMPTY_BUFFER)?.addListener(ChannelFutureListener.CLOSE)
    }

    @Throws(Exception::class)
    private fun doHandleForceOffReplyMessage(ctx: ChannelHandlerContext, msg: ForceOffReplyMessage) {
        logger.trace("doHandleForceOffReplyMessage# {}, {}", ctx, msg)
        ctx.channel()?.close()
    }

    @Throws(Exception::class)
    private fun TcpTunnel.handleTcpRequestMessage(ctx: ChannelHandlerContext, tunnelRequest: TunnelRequest) {
        requireNotRegistered(tunnelRequest.remotePort)
        val tunnelId = tunnelIds.nextId
        val sessionChannels = SessionChannels(tunnelId, tunnelRequest, ctx.channel())
        ctx.channel().attr(AK_SESSION_CHANNELS).set(sessionChannels)
        callback.onChannelConnected(ctx, startTunnel(null, tunnelRequest.remotePort, sessionChannels))
        ctx.channel().writeAndFlush(ProtoMessage.RESPONSE_OK(tunnelId, tunnelRequest))
    }

    @Throws(Exception::class)
    private fun HttpTunnel.handleHttpRequestMessage(ctx: ChannelHandlerContext, tunnelRequest: TunnelRequest) {
        requireNotRegistered(tunnelRequest.host)
        val tunnelId = tunnelIds.nextId
        val sessionChannels = SessionChannels(tunnelId, tunnelRequest, ctx.channel())
        ctx.channel().attr(AK_SESSION_CHANNELS).set(sessionChannels)
        callback.onChannelConnected(ctx, startTunnel(tunnelRequest.host, sessionChannels))
        ctx.channel().writeAndFlush(ProtoMessage.RESPONSE_OK(tunnelId, tunnelRequest))
    }

    interface Callback {
        fun onChannelInactive(ctx: ChannelHandlerContext, tcpFd: TcpFdDefaultImpl?)
        fun onChannelInactive(ctx: ChannelHandlerContext, httpFd: HttpFdDefaultImpl?)
        fun onChannelConnected(ctx: ChannelHandlerContext, tcpFd: TcpFdDefaultImpl?)
        fun onChannelConnected(ctx: ChannelHandlerContext, httpFd: HttpFdDefaultImpl?)
    }

}
