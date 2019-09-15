package tunnel2.server

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import tunnel2.common.TunnelException
import tunnel2.common.TunnelRequest
import tunnel2.common.TunnelType
import tunnel2.common.logging.LoggerFactory
import tunnel2.common.proto.ProtoCw
import tunnel2.common.proto.ProtoMessage
import tunnel2.server.http.HttpServer
import tunnel2.server.interceptor.TunnelRequestInterceptor
import tunnel2.server.internal.AK_SERVER_SESSION_CHANNELS
import tunnel2.server.internal.IdProducer
import tunnel2.server.internal.ServerSessionChannels
import tunnel2.server.tcp.TcpServer

class TunnelServerChannelHandler(
    private val tunnelRequestInterceptor: TunnelRequestInterceptor,
    private val tunnelIdProducer: IdProducer,
    private val tcpServer: TcpServer? = null,
    private val httpServer: HttpServer? = null,
    private val httpsServer: HttpServer? = null
) : SimpleChannelInboundHandler<ProtoMessage>() {

    companion object {
        private val logger = LoggerFactory.getLogger(TunnelServerChannelHandler::class.java)
    }

    override fun channelInactive(ctx: ChannelHandlerContext?) {
        logger.trace("channelInactive: {}", ctx)
        if (ctx == null) {
            super.channelInactive(ctx)
            return
        }
        ctx.channel().attr<ServerSessionChannels>(AK_SERVER_SESSION_CHANNELS).get()?.also {
            when (it.tunnelRequest.type) {
                TunnelType.TCP -> tcpServer?.registry?.unregister(it.tunnelId)
                TunnelType.HTTP -> httpServer?.registry?.unregister(it.tunnelRequest.host)
                TunnelType.HTTPS -> httpsServer?.registry?.unregister(it.tunnelRequest.host)
                else -> {
                    // Nothing
                }
            }
        }
        ctx.channel().attr<ServerSessionChannels>(AK_SERVER_SESSION_CHANNELS).set(null)
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
        when (msg.cw) {
            ProtoCw.PING -> handlePingMessage(ctx, msg)
            ProtoCw.REQUEST -> handleRequestMessage(ctx, msg)
            ProtoCw.TRANSFER -> handleTransferMessage(ctx, msg)
            ProtoCw.LOCAL_CONNECTED -> handleLocalConnectedMessage(ctx, msg)
            ProtoCw.LOCAL_DISCONNECT -> handleLocalDisconnectMessage(ctx, msg)
            else -> {
                // Nothing
            }
        }
    }

    private fun handlePingMessage(ctx: ChannelHandlerContext, msg: ProtoMessage) {
        logger.trace("handlePingMessage# {}, {}", ctx, msg)
        ctx.writeAndFlush(ProtoMessage(ProtoCw.PONG))
    }

    private fun handleRequestMessage(ctx: ChannelHandlerContext, msg: ProtoMessage) {
        logger.trace("handleRequestMessage# {}, {}", ctx, msg)
        try {
            val tunnelRequest = TunnelRequest.fromBytes(msg.data)
            logger.trace("tunnelRequest: {}", tunnelRequest)
            when (tunnelRequest.type) {
                TunnelType.TCP -> {
                    tcpServer?.also { handleTcpRequestMessage(ctx, it, tunnelRequest) }
                        ?: throw TunnelException("TCP协议隧道未开启")
                }
                TunnelType.HTTP -> {
                    httpServer?.also { handleHttpRequestMessage(ctx, it, tunnelRequest) }
                        ?: throw TunnelException("HTTP协议隧道未开启")
                }
                TunnelType.HTTPS -> {
                    httpsServer?.also { handleHttpRequestMessage(ctx, it, tunnelRequest) }
                        ?: throw TunnelException("HTTPS协议隧道未开启")
                }
                else -> throw TunnelException("不支持的隧道类型")
            }
        } catch (e: Exception) {
            ctx.channel().writeAndFlush(
                ProtoMessage(
                    ProtoCw.RESPONSE_ERR,
                    data = e.message.toString().toByteArray()
                )
            ).addListener(ChannelFutureListener.CLOSE)
        }
    }

    private fun handleTransferMessage(ctx: ChannelHandlerContext, msg: ProtoMessage) {
        val sessionChannels = ctx.channel().attr<ServerSessionChannels>(AK_SERVER_SESSION_CHANNELS).get() ?: return
        when (sessionChannels.tunnelRequest.type) {
            TunnelType.TCP -> {
                tcpServer ?: return
                tcpServer.registry.getSessionChannel(msg.tunnelId, msg.sessionId)?.writeAndFlush(
                    Unpooled.wrappedBuffer(msg.data)
                )
            }
            TunnelType.HTTP -> {
                httpServer ?: return
                httpServer.registry.getSessionChannel(msg.tunnelId, msg.sessionId)?.writeAndFlush(
                    Unpooled.wrappedBuffer(msg.data)
                )
            }
            TunnelType.HTTPS -> {
                httpsServer ?: return
                httpsServer.registry.getSessionChannel(msg.tunnelId, msg.sessionId)?.writeAndFlush(
                    Unpooled.wrappedBuffer(msg.data)
                )
            }
            else -> {
                // pass
            }
        }
    }

    private fun handleLocalConnectedMessage(ctx: ChannelHandlerContext, msg: ProtoMessage) {
        logger.trace("handleLocalConnectedMessage# {}, {}", ctx, msg)
        // 无须处理
    }

    private fun handleLocalDisconnectMessage(ctx: ChannelHandlerContext, msg: ProtoMessage) {
        logger.trace("handleLocalDisconnectMessage# {}, {}", ctx, msg)
        val sessionChannels = ctx.channel().attr<ServerSessionChannels>(AK_SERVER_SESSION_CHANNELS).get() ?: return
        val sessionChannel = sessionChannels.getSessionChannel(msg.sessionId)
        // 解决 HTTP/1.x 数据传输问题
        sessionChannel?.writeAndFlush(Unpooled.EMPTY_BUFFER)?.addListener(ChannelFutureListener.CLOSE)
    }

    // ==================================================================
    @Throws(TunnelException::class)
    private fun handleTcpRequestMessage(ctx: ChannelHandlerContext, server: TcpServer, request: TunnelRequest) {
        val tunnelRequest = tunnelRequestInterceptor.handleTunnelRequest(request)
        val tunnelId = tunnelIdProducer.nextId
        val sessionChannels = ServerSessionChannels(
            tunnelId,
            tunnelRequest,
            ctx.channel()
        )
        ctx.channel().attr<ServerSessionChannels>(AK_SERVER_SESSION_CHANNELS).set(sessionChannels)
        server.startTunnel(null, tunnelRequest.remotePort, sessionChannels)
        ctx.channel().writeAndFlush(
            ProtoMessage(
                ProtoCw.RESPONSE_OK,
                tunnelId = tunnelId,
                data = tunnelRequest.toBytes()
            )
        )
    }

    @Throws(TunnelException::class)
    private fun handleHttpRequestMessage(ctx: ChannelHandlerContext, server: HttpServer, request: TunnelRequest) {
        val tunnelRequest = tunnelRequestInterceptor.handleTunnelRequest(request)
        if (server.registry.isRegistered(tunnelRequest.host)) {
            throw TunnelException("host(${tunnelRequest.host}) already used")
        }
        val tunnelId = tunnelIdProducer.nextId
        val sessionChannels = ServerSessionChannels(tunnelId, tunnelRequest, ctx.channel())
        ctx.channel().attr<ServerSessionChannels>(AK_SERVER_SESSION_CHANNELS).set(sessionChannels)
        server.registry.register(tunnelRequest.host, sessionChannels)
        ctx.channel().writeAndFlush(
            ProtoMessage(
                ProtoCw.RESPONSE_OK,
                tunnelId = tunnelId,
                data = tunnelRequest.toBytes()
            )
        )
    }

}