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
import lighttunnel.server.http.HttpRegistry
import lighttunnel.server.interceptor.TunnelRequestInterceptor
import lighttunnel.server.tcp.TcpRegistry
import lighttunnel.server.tcp.TcpServer
import lighttunnel.server.util.AttributeKeys
import lighttunnel.server.util.SessionChannels
import lighttunnel.util.IncIds
import lighttunnel.util.LongUtil

class TunnelServerChannelHandler(
    private val tunnelRequestInterceptor: TunnelRequestInterceptor,
    private val tunnelIds: IncIds,
    // tcp
    private val tcpServer: TcpServer? = null,
    private val tcpRegistry: TcpRegistry? = null,
    // http
    private val httpRegistry: HttpRegistry? = null,
    // https
    private val httpsRegistry: HttpRegistry? = null
) : SimpleChannelInboundHandler<ProtoMessage>() {
    private val logger by loggerDelegate()

    override fun channelInactive(ctx: ChannelHandlerContext?) {
        logger.trace("channelInactive: {}", ctx)
        if (ctx == null) {
            super.channelInactive(ctx)
            return
        }
        ctx.channel().attr(AttributeKeys.AK_SESSION_CHANNELS).get()?.also { sc ->
            when (sc.tunnelRequest.type) {
                TunnelRequest.Type.TCP -> tcpRegistry?.unregister(sc.tunnelRequest.remotePort)
                TunnelRequest.Type.HTTP -> httpRegistry?.unregister(sc.tunnelRequest.host)
                TunnelRequest.Type.HTTPS -> httpsRegistry?.unregister(sc.tunnelRequest.host)
                else -> {
                    // Nothing
                }
            }
        }
        ctx.channel().attr(AttributeKeys.AK_SESSION_CHANNELS).set(null)
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
                    val registry = tcpRegistry ?: throw ProtoException("TCP协议隧道未开启")
                    server.handleTcpRequestMessage(ctx, registry, finalTunnelRequest)
                }
                TunnelRequest.Type.HTTP -> {
                    val registry = httpRegistry ?: throw ProtoException("HTTP协议隧道未开启")
                    handleHttpRequestMessage(ctx, registry, finalTunnelRequest)
                }
                TunnelRequest.Type.HTTPS -> {
                    val registry = httpsRegistry ?: throw ProtoException("HTTPS协议隧道未开启")
                    handleHttpRequestMessage(ctx, registry, finalTunnelRequest)
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
        val sessionChannels = ctx.channel().attr(AttributeKeys.AK_SESSION_CHANNELS).get() ?: return
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
        val sessionChannels = ctx.channel().attr(AttributeKeys.AK_SESSION_CHANNELS).get() ?: return
        val sessionChannel = sessionChannels.removeChannel(msg.sessionId)
        // 解决 HTTP/1.x 数据传输问题
        sessionChannel?.writeAndFlush(Unpooled.EMPTY_BUFFER)?.addListener(ChannelFutureListener.CLOSE)
    }

    @Throws(Exception::class)
    private fun TcpServer.handleTcpRequestMessage(ctx: ChannelHandlerContext, registry: TcpRegistry, tunnelRequest: TunnelRequest) {
        if (registry.isRegistered(tunnelRequest.remotePort)) {
            throw ProtoException("remotePort(${tunnelRequest.remotePort}) already used")
        }
        val tunnelId = tunnelIds.nextId
        val sessionChannels = SessionChannels(tunnelId, tunnelRequest, ctx.channel())
        ctx.channel().attr(AttributeKeys.AK_SESSION_CHANNELS).set(sessionChannels)
        this.startTunnel(null, tunnelRequest.remotePort, sessionChannels)
        val head = LongUtil.toBytes(tunnelId, 0L)
        val data = tunnelRequest.toBytes()
        ctx.channel().writeAndFlush(ProtoMessage(ProtoMessageType.RESPONSE_OK, head, data))
    }

    @Throws(Exception::class)
    private fun handleHttpRequestMessage(ctx: ChannelHandlerContext, registry: HttpRegistry, tunnelRequest: TunnelRequest) {
        if (registry.isRegistered(tunnelRequest.host)) {
            throw ProtoException("host(${tunnelRequest.host}) already used")
        }
        val tunnelId = tunnelIds.nextId
        val sessionChannels = SessionChannels(tunnelId, tunnelRequest, ctx.channel())
        ctx.channel().attr(AttributeKeys.AK_SESSION_CHANNELS).set(sessionChannels)
        registry.register(tunnelRequest.host, sessionChannels)
        val head = LongUtil.toBytes(tunnelId, 0L)
        val data = tunnelRequest.toBytes()
        ctx.channel().writeAndFlush(ProtoMessage(ProtoMessageType.RESPONSE_OK, head, data))
    }

}