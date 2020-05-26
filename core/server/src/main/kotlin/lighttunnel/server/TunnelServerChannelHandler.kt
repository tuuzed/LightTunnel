package lighttunnel.server

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import lighttunnel.logger.loggerDelegate
import lighttunnel.proto.ProtoException
import lighttunnel.proto.ProtoMessage
import lighttunnel.proto.ProtoMessageType
import lighttunnel.proto.TunnelRequest
import lighttunnel.server.http.HttpFd
import lighttunnel.server.http.HttpServer
import lighttunnel.server.tcp.TcpFd
import lighttunnel.server.tcp.TcpServer
import lighttunnel.server.util.AK_SESSION_CHANNELS
import lighttunnel.server.util.SessionChannels
import lighttunnel.util.IncIds
import lighttunnel.util.LongUtil

internal class TunnelServerChannelHandler(
    private val tunnelRequestInterceptor: TunnelRequestInterceptor,
    private val tunnelIds: IncIds,
    private val tcpServer: TcpServer? = null,
    private val httpServer: HttpServer? = null,
    private val httpsServer: HttpServer? = null,
    private val onChannelStateListener: OnChannelStateListener? = null
) : SimpleChannelInboundHandler<ProtoMessage>() {
    private val logger by loggerDelegate()

    override fun channelInactive(ctx: ChannelHandlerContext?) {
        logger.trace("channelInactive: {}", ctx)
        if (ctx == null) {
            super.channelInactive(ctx)
            return
        }
        ctx.channel().attr(AK_SESSION_CHANNELS).get()?.also { sc ->
            when (sc.tunnelRequest.type) {
                TunnelRequest.Type.TCP -> {
                    val fd = tcpServer?.stopTunnel(sc.tunnelRequest.remotePort)
                    onChannelStateListener?.onChannelInactive(ctx, fd)
                }
                TunnelRequest.Type.HTTP -> {
                    val fd = httpServer?.stopTunnel(sc.tunnelRequest.host)
                    onChannelStateListener?.onChannelInactive(ctx, fd)
                }
                TunnelRequest.Type.HTTPS -> {
                    val fd = httpsServer?.stopTunnel(sc.tunnelRequest.host)
                    onChannelStateListener?.onChannelInactive(ctx, fd)
                }
                else -> {
                    // Nothing
                }
            }
        }
        ctx.channel().attr(AK_SESSION_CHANNELS).set(null)
        super.channelInactive(ctx)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
        logger.trace("exceptionCaught: {}", ctx, cause)
        ctx ?: return
        ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
    }

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
            ProtoMessageType.FORCED_OFFLINE_REPLY -> doHandleForcedOfflineReplyMessage(ctx, msg)
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
            logger.trace("originalTunnelRequest: {}, finalTunnelRequest: {}", originalTunnelRequest, finalTunnelRequest)
            when (finalTunnelRequest.type) {
                TunnelRequest.Type.TCP -> {
                    val server = tcpServer ?: throw ProtoException("TCP协议隧道未开启")
                    server.handleTcpRequestMessage(ctx, finalTunnelRequest)
                }
                TunnelRequest.Type.HTTP -> {
                    val server = httpServer ?: throw ProtoException("HTTP协议隧道未开启")
                    server.handleHttpRequestMessage(ctx, finalTunnelRequest)
                }
                TunnelRequest.Type.HTTPS -> {
                    val server = httpsServer ?: throw ProtoException("HTTPS协议隧道未开启")
                    server.handleHttpRequestMessage(ctx, finalTunnelRequest)
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

    @Suppress("DuplicatedCode")
    @Throws(Exception::class)
    private fun TcpServer.handleTcpRequestMessage(ctx: ChannelHandlerContext, tunnelRequest: TunnelRequest) {
        val tunnelId = tunnelIds.nextId
        val sessionChannels = SessionChannels(tunnelId, tunnelRequest, ctx.channel())
        ctx.channel().attr(AK_SESSION_CHANNELS).set(sessionChannels)
        val fd = this.startTunnel(null, tunnelRequest.remotePort, sessionChannels)
        onChannelStateListener?.onChannelConnected(ctx, fd)
        val head = LongUtil.toBytes(tunnelId, 0L)
        val data = tunnelRequest.toBytes()
        ctx.channel().writeAndFlush(ProtoMessage(ProtoMessageType.RESPONSE_OK, head, data))
    }

    @Suppress("DuplicatedCode")
    @Throws(Exception::class)
    private fun HttpServer.handleHttpRequestMessage(ctx: ChannelHandlerContext, tunnelRequest: TunnelRequest) {
        val tunnelId = tunnelIds.nextId
        val sessionChannels = SessionChannels(tunnelId, tunnelRequest, ctx.channel())
        ctx.channel().attr(AK_SESSION_CHANNELS).set(sessionChannels)
        val fd = this.startTunnel(tunnelRequest.host, sessionChannels)
        onChannelStateListener?.onChannelConnected(ctx, fd)
        val head = LongUtil.toBytes(tunnelId, 0L)
        val data = tunnelRequest.toBytes()
        ctx.channel().writeAndFlush(ProtoMessage(ProtoMessageType.RESPONSE_OK, head, data))
    }

    internal interface OnChannelStateListener {
        fun onChannelInactive(ctx: ChannelHandlerContext, tcpFd: TcpFd?) {}
        fun onChannelInactive(ctx: ChannelHandlerContext, httpFd: HttpFd?) {}
        fun onChannelConnected(ctx: ChannelHandlerContext, tcpFd: TcpFd?) {}
        fun onChannelConnected(ctx: ChannelHandlerContext, httpFd: HttpFd?) {}
    }

}