package lighttunnel.server

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import lighttunnel.base.proto.ProtoMessage
import lighttunnel.base.util.IncIds
import lighttunnel.base.util.loggerDelegate
import lighttunnel.openapi.ProtoException
import lighttunnel.openapi.TunnelRequest
import lighttunnel.openapi.TunnelRequestInterceptor
import lighttunnel.openapi.TunnelType
import lighttunnel.server.http.HttpFdDefaultImpl
import lighttunnel.server.http.HttpTunnel
import lighttunnel.server.tcp.TcpFdDefaultImpl
import lighttunnel.server.tcp.TcpTunnel
import lighttunnel.server.util.AK_SESSION_CHANNELS
import lighttunnel.server.util.SessionChannels

internal abstract class TunnelServerDaemonChannelHandler(
    private val tunnelRequestInterceptor: TunnelRequestInterceptor?,
    private val tunnelIds: IncIds,
    private val tcpTunnel: TcpTunnel? = null,
    private val httpTunnel: HttpTunnel? = null,
    private val httpsTunnel: HttpTunnel? = null
) : SimpleChannelInboundHandler<ProtoMessage>() {

    private val logger by loggerDelegate()

    @Throws(Exception::class)
    override fun channelInactive(ctx: ChannelHandlerContext?) {
        logger.trace("channelInactive: {}", ctx)
        if (ctx == null) {
            super.channelInactive(ctx)
            return
        }
        ctx.channel().attr(AK_SESSION_CHANNELS).get()?.also { sc ->
            when (sc.tunnelRequest.tunnelType) {
                TunnelType.TCP -> onChannelInactive(ctx, tcpTunnel?.stopTunnel(sc.tunnelRequest.remotePort))
                TunnelType.HTTP -> onChannelInactive(ctx, httpTunnel?.stopTunnel(sc.tunnelRequest.host))
                TunnelType.HTTPS -> onChannelInactive(ctx, httpsTunnel?.stopTunnel(sc.tunnelRequest.host))
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
            ProtoMessage.Type.PING -> doHandlePingMessage(ctx, msg)
            ProtoMessage.Type.REQUEST -> doHandleRequestMessage(ctx, msg)
            ProtoMessage.Type.TRANSFER -> doHandleTransferMessage(ctx, msg)
            ProtoMessage.Type.LOCAL_CONNECTED -> doHandleLocalConnectedMessage(ctx, msg)
            ProtoMessage.Type.LOCAL_DISCONNECT -> doHandleLocalDisconnectMessage(ctx, msg)
            ProtoMessage.Type.FORCE_OFF_REPLY -> doHandleForceOffReplyMessage(ctx, msg)
            else -> {
                // Nothing
            }
        }
    }

    @Throws(Exception::class)
    private fun doHandlePingMessage(ctx: ChannelHandlerContext, msg: ProtoMessage) {
        logger.trace("doHandlePingMessage# {}, {}", ctx, msg)
        ctx.writeAndFlush(ProtoMessage.PONG())
    }

    @Throws(Exception::class)
    private fun doHandleRequestMessage(ctx: ChannelHandlerContext, msg: ProtoMessage) {
        logger.trace("doHandleRequestMessage# {}, {}", ctx, msg)
        try {
            val originalTunnelRequest = TunnelRequest.fromBytes(msg.data)
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
    private fun doHandleTransferMessage(ctx: ChannelHandlerContext, msg: ProtoMessage) {
        logger.trace("doHandleTransferMessage# {}, {}", ctx, msg)
        val sessionChannels = ctx.channel().attr(AK_SESSION_CHANNELS).get() ?: return
        val sessionChannel = sessionChannels.getChannel(msg.sessionId)
        sessionChannel?.writeAndFlush(Unpooled.wrappedBuffer(msg.data))
    }

    @Throws(Exception::class)
    private fun doHandleLocalConnectedMessage(ctx: ChannelHandlerContext, msg: ProtoMessage) {
        logger.trace("doHandleLocalConnectedMessage# {}, {}", ctx, msg)
        // 无须处理
    }

    @Throws(Exception::class)
    private fun doHandleLocalDisconnectMessage(ctx: ChannelHandlerContext, msg: ProtoMessage) {
        logger.trace("doHandleLocalDisconnectMessage# {}, {}", ctx, msg)
        val sessionChannels = ctx.channel().attr(AK_SESSION_CHANNELS).get() ?: return
        val sessionChannel = sessionChannels.removeChannel(msg.sessionId)
        // 解决 HTTP/1.x 数据传输问题
        sessionChannel?.writeAndFlush(Unpooled.EMPTY_BUFFER)?.addListener(ChannelFutureListener.CLOSE)
    }


    @Throws(Exception::class)
    private fun doHandleForceOffReplyMessage(ctx: ChannelHandlerContext, msg: ProtoMessage) {
        logger.trace("doHandleForceOffReplyMessage# {}, {}", ctx, msg)
        ctx.channel()?.close()
    }

    @Throws(Exception::class)
    private fun TcpTunnel.handleTcpRequestMessage(ctx: ChannelHandlerContext, tunnelRequest: TunnelRequest) {
        requireNotRegistered(tunnelRequest.remotePort)
        val tunnelId = tunnelIds.nextId
        val sessionChannels = SessionChannels(tunnelId, tunnelRequest, ctx.channel())
        ctx.channel().attr(AK_SESSION_CHANNELS).set(sessionChannels)
        onChannelConnected(ctx, startTunnel(null, tunnelRequest.remotePort, sessionChannels))
        ctx.channel().writeAndFlush(ProtoMessage.RESPONSE_OK(tunnelId, tunnelRequest))
    }

    @Throws(Exception::class)
    private fun HttpTunnel.handleHttpRequestMessage(ctx: ChannelHandlerContext, tunnelRequest: TunnelRequest) {
        requireNotRegistered(tunnelRequest.host)
        val tunnelId = tunnelIds.nextId
        val sessionChannels = SessionChannels(tunnelId, tunnelRequest, ctx.channel())
        ctx.channel().attr(AK_SESSION_CHANNELS).set(sessionChannels)
        onChannelConnected(ctx, startTunnel(tunnelRequest.host, sessionChannels))
        ctx.channel().writeAndFlush(ProtoMessage.RESPONSE_OK(tunnelId, tunnelRequest))
    }

    abstract fun onChannelInactive(ctx: ChannelHandlerContext, tcpFd: TcpFdDefaultImpl?)
    abstract fun onChannelInactive(ctx: ChannelHandlerContext, httpFd: HttpFdDefaultImpl?)
    abstract fun onChannelConnected(ctx: ChannelHandlerContext, tcpFd: TcpFdDefaultImpl?)
    abstract fun onChannelConnected(ctx: ChannelHandlerContext, httpFd: HttpFdDefaultImpl?)

}