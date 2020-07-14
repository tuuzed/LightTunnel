package lighttunnel.server

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import lighttunnel.base.logger.loggerDelegate
import lighttunnel.base.proto.ProtoMessage
import lighttunnel.base.proto.ProtoMessageType
import lighttunnel.base.util.IncIds
import lighttunnel.base.util.LongUtil
import lighttunnel.openapi.ProtoException
import lighttunnel.openapi.TunnelRequest
import lighttunnel.openapi.TunnelRequestInterceptor
import lighttunnel.server.http.HttpFdDefaultImpl
import lighttunnel.server.http.HttpTunnel
import lighttunnel.server.tcp.TcpFdDefaultImpl
import lighttunnel.server.tcp.TcpTunnel
import lighttunnel.server.util.AK_SESSION_CHANNELS
import lighttunnel.server.util.SessionChannels

internal abstract class TunnelServerDaemonChannelHandler(
    private val tunnelRequestInterceptor: TunnelRequestInterceptor,
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
            when (sc.tunnelRequest.type) {
                TunnelRequest.Type.TCP -> onChannelInactive(ctx, tcpTunnel?.stopTunnel(sc.tunnelRequest.remotePort))
                TunnelRequest.Type.HTTP -> onChannelInactive(ctx, httpTunnel?.stopTunnel(sc.tunnelRequest.host))
                TunnelRequest.Type.HTTPS -> onChannelInactive(ctx, httpsTunnel?.stopTunnel(sc.tunnelRequest.host))
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
        ctx?.apply { channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE) }
    }

    @Throws(Exception::class)
    override fun channelRead0(ctx: ChannelHandlerContext?, msg: ProtoMessage?) {
        logger.trace("channelRead0: {}", ctx)
        ctx ?: return
        msg ?: return
        when (msg.type) {
            ProtoMessageType.PING -> doHandlePingMessage(ctx, msg)
            ProtoMessageType.REQUEST -> doHandleRequestMessage(ctx, msg)
            ProtoMessageType.TRANSFER -> doHandleTransferMessage(ctx, msg)
            ProtoMessageType.LOCAL_CONNECTED -> doHandleLocalConnectedMessage(ctx, msg)
            ProtoMessageType.LOCAL_DISCONNECT -> doHandleLocalDisconnectMessage(ctx, msg)
            ProtoMessageType.FORCE_OFF_REPLY -> doHandleForcedOfflineReplyMessage(ctx, msg)
            else -> {
                // Nothing
            }
        }
    }

    @Throws(Exception::class)
    private fun doHandlePingMessage(ctx: ChannelHandlerContext, msg: ProtoMessage) {
        logger.trace("doHandlePingMessage# {}, {}", ctx, msg)
        ctx.writeAndFlush(ProtoMessage(ProtoMessageType.PONG))
    }

    @Throws(Exception::class)
    private fun doHandleRequestMessage(ctx: ChannelHandlerContext, msg: ProtoMessage) {
        logger.trace("doHandleRequestMessage# {}, {}", ctx, msg)
        try {
            val originalTunnelRequest = TunnelRequest.fromBytes(msg.head)
            val finalTunnelRequest = tunnelRequestInterceptor.handleTunnelRequest(originalTunnelRequest)
            logger.trace("TunnelRequest=> original: {}, final: {}", originalTunnelRequest, finalTunnelRequest)
            when (finalTunnelRequest.type) {
                TunnelRequest.Type.TCP -> {
                    val tcpTunnel = tcpTunnel ?: throw ProtoException("TCP协议隧道未开启")
                    tcpTunnel.handleTcpRequestMessage(ctx, finalTunnelRequest)
                }
                TunnelRequest.Type.HTTP -> {
                    val httpTunnel = httpTunnel ?: throw ProtoException("HTTP协议隧道未开启")
                    httpTunnel.handleHttpRequestMessage(ctx, finalTunnelRequest)
                }
                TunnelRequest.Type.HTTPS -> {
                    val httpsTunnel = httpsTunnel ?: throw ProtoException("HTTPS协议隧道未开启")
                    httpsTunnel.handleHttpRequestMessage(ctx, finalTunnelRequest)
                }
                else -> throw ProtoException("不支持的隧道类型")
            }
        } catch (e: Exception) {
            ctx.channel().writeAndFlush(
                ProtoMessage(ProtoMessageType.RESPONSE_ERR, e.message.toString().toByteArray())
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
    private fun doHandleForcedOfflineReplyMessage(ctx: ChannelHandlerContext, msg: ProtoMessage) {
        logger.trace("doHandleForcedOfflineReplyMessage# {}, {}", ctx, msg)
        ctx.channel()?.close()
    }

    @Throws(Exception::class)
    private fun TcpTunnel.handleTcpRequestMessage(ctx: ChannelHandlerContext, tunnelRequest: TunnelRequest) {
        requireNotRegistered(tunnelRequest.remotePort)
        val tunnelId = tunnelIds.nextId
        val sessionChannels = SessionChannels(tunnelId, tunnelRequest, ctx.channel())
        ctx.channel().attr(AK_SESSION_CHANNELS).set(sessionChannels)
        onChannelConnected(ctx, startTunnel(null, tunnelRequest.remotePort, sessionChannels))
        val head = LongUtil.toBytes(tunnelId, 0L)
        val data = tunnelRequest.toBytes()
        ctx.channel().writeAndFlush(ProtoMessage(ProtoMessageType.RESPONSE_OK, head, data))
    }

    @Throws(Exception::class)
    private fun HttpTunnel.handleHttpRequestMessage(ctx: ChannelHandlerContext, tunnelRequest: TunnelRequest) {
        requireNotRegistered(tunnelRequest.host)
        val tunnelId = tunnelIds.nextId
        val sessionChannels = SessionChannels(tunnelId, tunnelRequest, ctx.channel())
        ctx.channel().attr(AK_SESSION_CHANNELS).set(sessionChannels)
        onChannelConnected(ctx, startTunnel(tunnelRequest.host, sessionChannels))
        val head = LongUtil.toBytes(tunnelId, 0L)
        val data = tunnelRequest.toBytes()
        ctx.channel().writeAndFlush(ProtoMessage(ProtoMessageType.RESPONSE_OK, head, data))
    }

    abstract fun onChannelInactive(ctx: ChannelHandlerContext, tcpFd: TcpFdDefaultImpl?)
    abstract fun onChannelInactive(ctx: ChannelHandlerContext, httpFd: HttpFdDefaultImpl?)
    abstract fun onChannelConnected(ctx: ChannelHandlerContext, tcpFd: TcpFdDefaultImpl?)
    abstract fun onChannelConnected(ctx: ChannelHandlerContext, httpFd: HttpFdDefaultImpl?)

}