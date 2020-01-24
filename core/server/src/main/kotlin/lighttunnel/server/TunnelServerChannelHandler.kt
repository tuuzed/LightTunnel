package lighttunnel.server

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import lighttunnel.logger.loggerDelegate
import lighttunnel.proto.ProtoMessageType
import lighttunnel.proto.ProtoException
import lighttunnel.proto.ProtoMessage
import lighttunnel.proto.TunnelRequest
import lighttunnel.server.http.HttpServer
import lighttunnel.server.interceptor.TunnelRequestInterceptor
import lighttunnel.server.tcp.TcpServer
import lighttunnel.server.util.AttributeKeys
import lighttunnel.server.util.IncIds
import lighttunnel.server.util.SessionChannels
import lighttunnel.util.LongUtil

class TunnelServerChannelHandler(
    private val tunnelRequestInterceptor: TunnelRequestInterceptor,
    private val tunnelIds: IncIds,
    private val tcpServer: TcpServer? = null,
    private val httpServer: HttpServer? = null,
    private val httpsServer: HttpServer? = null
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
                TunnelRequest.Type.TCP -> tcpServer?.also { it.registry.unregister(sc.tunnelRequest.remotePort) }
                TunnelRequest.Type.HTTP -> httpServer?.also { it.registry.unregister(sc.tunnelRequest.host) }
                TunnelRequest.Type.HTTPS -> httpsServer?.also { it.registry.unregister(sc.tunnelRequest.host) }
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
        logger.trace("handlePingMessage# {}, {}", ctx, msg)
        ctx.writeAndFlush(ProtoMessage(ProtoMessageType.PONG))
    }

    @Throws(Exception::class)
    private fun doHandleRequestMessage(ctx: ChannelHandlerContext, msg: ProtoMessage) {
        logger.trace("handleRequestMessage# {}, {}", ctx, msg)
        try {
            val tunnelRequest = TunnelRequest.fromBytes(msg.head)
            logger.trace("tunnelRequest: {}", tunnelRequest)
            when (tunnelRequest.type) {
                TunnelRequest.Type.TCP -> {
                    tcpServer?.also { handleTcpRequestMessage(ctx, it, tunnelRequest) }
                        ?: throw ProtoException("TCP协议隧道未开启")
                }
                TunnelRequest.Type.HTTP -> {
                    httpServer?.also { handleHttpRequestMessage(ctx, it, tunnelRequest) }
                        ?: throw ProtoException("HTTP协议隧道未开启")
                }
                TunnelRequest.Type.HTTPS -> {
                    httpsServer?.also { handleHttpRequestMessage(ctx, it, tunnelRequest) }
                        ?: throw ProtoException("HTTPS协议隧道未开启")
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
        val sessionPool = ctx.channel().attr(AttributeKeys.AK_SESSION_CHANNELS).get() ?: return
        when (sessionPool.tunnelRequest.type) {
            TunnelRequest.Type.TCP -> {
                tcpServer ?: return
                tcpServer.registry
                    .getSessionChannel(msg.tunnelId, msg.sessionId)
                    ?.writeAndFlush(Unpooled.wrappedBuffer(msg.data))
            }
            TunnelRequest.Type.HTTP -> {
                httpServer ?: return
                httpServer.registry
                    .getSessionChannel(msg.tunnelId, msg.sessionId)
                    ?.writeAndFlush(Unpooled.wrappedBuffer(msg.data))
            }
            TunnelRequest.Type.HTTPS -> {
                httpsServer ?: return
                httpsServer.registry
                    .getSessionChannel(msg.tunnelId, msg.sessionId)
                    ?.writeAndFlush(Unpooled.wrappedBuffer(msg.data))
            }
            else -> {
            }
        }
    }

    @Throws(Exception::class)
    private fun doHandleLocalConnectedMessage(ctx: ChannelHandlerContext, msg: ProtoMessage) {
        logger.trace("handleLocalConnectedMessage# {}, {}", ctx, msg)
        // 无须处理
    }

    @Throws(Exception::class)
    private fun doHandleLocalDisconnectMessage(ctx: ChannelHandlerContext, msg: ProtoMessage) {
        logger.trace("handleLocalDisconnectMessage# {}, {}", ctx, msg)
        val sessionChannels = ctx.channel().attr(AttributeKeys.AK_SESSION_CHANNELS).get() ?: return
        val sessionChannel = sessionChannels.getChannel(msg.sessionId)
        // 解决 HTTP/1.x 数据传输问题
        sessionChannel?.writeAndFlush(Unpooled.EMPTY_BUFFER)?.addListener(ChannelFutureListener.CLOSE)
    }

    @Throws(Exception::class)
    private fun handleTcpRequestMessage(ctx: ChannelHandlerContext, server: TcpServer, tunnelRequest: TunnelRequest) {
        @Suppress("NAME_SHADOWING")
        val tunnelRequest = tunnelRequestInterceptor.handleTunnelRequest(tunnelRequest)
        val tunnelId = tunnelIds.nextId
        val sessionChannels = SessionChannels(tunnelId, tunnelRequest, ctx.channel())
        ctx.channel().attr(AttributeKeys.AK_SESSION_CHANNELS).set(sessionChannels)
        server.startTunnel(null, tunnelRequest.remotePort, sessionChannels)
        val head = LongUtil.toBytes(tunnelId, 0L)
        val data = tunnelRequest.toBytes()
        ctx.channel().writeAndFlush(ProtoMessage(ProtoMessageType.RESPONSE_OK, head, data))
    }

    @Throws(Exception::class)
    private fun handleHttpRequestMessage(ctx: ChannelHandlerContext, server: HttpServer, tunnelRequest: TunnelRequest) {
        @Suppress("NAME_SHADOWING")
        val tunnelRequest = tunnelRequestInterceptor.handleTunnelRequest(tunnelRequest)
        if (server.registry.isRegistered(tunnelRequest.host)) {
            throw ProtoException("host(${tunnelRequest.host}) already used")
        }
        val tunnelId = tunnelIds.nextId
        val sessionChannels = SessionChannels(tunnelId, tunnelRequest, ctx.channel())
        ctx.channel().attr(AttributeKeys.AK_SESSION_CHANNELS).set(sessionChannels)
        server.registry.register(tunnelRequest.host, sessionChannels)
        val head = LongUtil.toBytes(tunnelId, 0L)
        val data = tunnelRequest.toBytes()
        ctx.channel().writeAndFlush(ProtoMessage(ProtoMessageType.RESPONSE_OK, head, data))
    }

}