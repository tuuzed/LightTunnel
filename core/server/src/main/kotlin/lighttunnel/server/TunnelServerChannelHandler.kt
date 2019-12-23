package lighttunnel.server

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import lighttunnel.logger.loggerDelegate
import lighttunnel.proto.ProtoCommand
import lighttunnel.proto.ProtoException
import lighttunnel.proto.ProtoMessage
import lighttunnel.proto.TunnelRequest
import lighttunnel.server.http.HttpServer
import lighttunnel.server.interceptor.TunnelRequestInterceptor
import lighttunnel.server.tcp.TcpServer
import lighttunnel.server.util.AttributeKeys
import lighttunnel.server.util.IncIds
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
        ctx.channel().attr(AttributeKeys.AK_SESSION_CHANNELS).get()?.also {
            when (it.request.type) {
                TunnelRequest.Type.TCP -> tcpServer?.registry?.unregister(it.tunnelId)
                TunnelRequest.Type.HTTP -> httpServer?.registry?.unregister(it.request.host)
                TunnelRequest.Type.HTTPS -> httpsServer?.registry?.unregister(it.request.host)
                else -> {
                }
            }
        }
        ctx.channel().attr(AttributeKeys.AK_SESSION_CHANNELS).set(null)
        super.channelInactive(ctx)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
        ctx ?: return
        logger.trace("exceptionCaught: {}", ctx, cause)
        ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
    }

    override fun channelRead0(ctx: ChannelHandlerContext?, msg: ProtoMessage?) {
        ctx ?: return
        msg ?: return
        when (msg.cmd) {
            ProtoCommand.PING -> doHandlePingMessage(ctx, msg)
            ProtoCommand.REQUEST -> doHandleRequestMessage(ctx, msg)
            ProtoCommand.TRANSFER -> doHandleTransferMessage(ctx, msg)
            ProtoCommand.LOCAL_CONNECTED -> doHandleLocalConnectedMessage(ctx, msg)
            ProtoCommand.LOCAL_DISCONNECT -> doHandleLocalDisconnectMessage(ctx, msg)
            else -> {
                // Nothing
            }
        }
    }

    @Throws(Exception::class)
    private fun doHandlePingMessage(ctx: ChannelHandlerContext, msg: ProtoMessage) {
        logger.trace("handlePingMessage# {}, {}", ctx, msg)
        ctx.writeAndFlush(ProtoMessage(ProtoCommand.PONG))
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
                ProtoMessage(ProtoCommand.RESPONSE_ERR, e.message.toString().toByteArray())
            ).addListener(ChannelFutureListener.CLOSE)
        }
    }

    @Throws(Exception::class)
    private fun doHandleTransferMessage(ctx: ChannelHandlerContext, msg: ProtoMessage) {
        val sessionPool = ctx.channel().attr(AttributeKeys.AK_SESSION_CHANNELS).get() ?: return
        when (sessionPool.request.type) {
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
        val sessionPool = ctx.channel().attr(AttributeKeys.AK_SESSION_CHANNELS).get() ?: return
        val sessionChannel = sessionPool.getChannel(msg.sessionId)
        // 解决 HTTP/1.x 数据传输问题
        sessionChannel?.writeAndFlush(Unpooled.EMPTY_BUFFER)?.addListener(ChannelFutureListener.CLOSE)
    }

    @Throws(Exception::class)
    private fun handleTcpRequestMessage(ctx: ChannelHandlerContext, server: TcpServer, tpRequest: TunnelRequest) {
        val tunnelRequest = tunnelRequestInterceptor.handleTunnelRequest(tpRequest)
        val tunnelId = tunnelIds.nextId
        val sessionPool = SessionChannels(tunnelId, tunnelRequest, ctx.channel())
        ctx.channel().attr(AttributeKeys.AK_SESSION_CHANNELS).set(sessionPool)
        server.startTunnel(null, tunnelRequest.remotePort, sessionPool)
        val head = LongUtil.toBytes(tunnelId, 0L)
        val data = tunnelRequest.toBytes()
        ctx.channel().writeAndFlush(ProtoMessage(ProtoCommand.RESPONSE_OK, head, data))
    }

    @Throws(Exception::class)
    private fun handleHttpRequestMessage(ctx: ChannelHandlerContext, server: HttpServer, request: TunnelRequest) {
        @Suppress("NAME_SHADOWING")
        val request = tunnelRequestInterceptor.handleTunnelRequest(request)
        if (server.registry.isRegistered(request.host)) {
            throw ProtoException("host(${request.host}) already used")
        }
        val tunnelId = tunnelIds.nextId
        val sessionPool = SessionChannels(tunnelId, request, ctx.channel())
        ctx.channel().attr(AttributeKeys.AK_SESSION_CHANNELS).set(sessionPool)
        server.registry.register(request.host, sessionPool)
        val head = LongUtil.toBytes(tunnelId, 0L)
        val data = request.toBytes()
        ctx.channel().writeAndFlush(ProtoMessage(ProtoCommand.RESPONSE_OK, head, data))
    }

}