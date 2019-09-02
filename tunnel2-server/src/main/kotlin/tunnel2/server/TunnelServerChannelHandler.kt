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
import tunnel2.server.udp.UdpServer
import java.nio.charset.StandardCharsets

class TunnelServerChannelHandler(
    private val tunnelRequestInterceptor: TunnelRequestInterceptor,
    private val tunnelIdProducer: IdProducer,
    private val tcpServer: TcpServer? = null,
    private val udpServer: UdpServer? = null,
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
                TunnelType.UDP -> TODO("shutdown")
                TunnelType.HTTP -> httpServer?.registry?.unregister(it.tunnelRequest.vhost)
                TunnelType.HTTPS -> httpsServer?.registry?.unregister(it.tunnelRequest.vhost)
                else -> {
                    // pass
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
                // pass
            }
        }
    }

    private fun handlePingMessage(ctx: ChannelHandlerContext, msg: ProtoMessage) {
        logger.trace("handlePingMessage# {}, {}", ctx, msg)
        ctx.writeAndFlush(ProtoMessage(ProtoCw.PONG))
    }

    private fun handleRequestMessage(ctx: ChannelHandlerContext, msg: ProtoMessage) {
        logger.trace("handleRequestMessage# {}, {}", ctx, msg)
        val tunnelRequest = TunnelRequest.fromBytes(msg.data)
        when (tunnelRequest.type) {
            TunnelType.TCP -> {
                if (tcpServer != null) {
                    handleTcpRequestMessage(ctx, tcpServer, tunnelRequest)
                }
            }
            TunnelType.HTTP -> {
                if (httpServer != null) {
                    handleHttpRequestMessage(ctx, httpServer, tunnelRequest)
                }

            }
            TunnelType.HTTPS -> {
                if (httpsServer != null) {
                    handleHttpRequestMessage(ctx, httpsServer, tunnelRequest)
                }
            }
            else -> {
                val head = Unpooled.copyBoolean(false)
                ctx.channel().writeAndFlush(
                    ProtoMessage(
                        ProtoCw.RESPONSE_ERR,
                        0,
                        0,
                        "协议错误".toByteArray(StandardCharsets.UTF_8)
                    )
                ).addListener(ChannelFutureListener.CLOSE)
                head.release()
            }
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

    private fun handleTcpRequestMessage(ctx: ChannelHandlerContext, server: TcpServer, request: TunnelRequest) {
        try {
            val tunnelRequest = tunnelRequestInterceptor.handleTunnelRequest(request)
            val tunnelId = tunnelIdProducer.nextId
            val sessionChannels = ServerSessionChannels(
                tunnelId,
                tunnelRequest,
                ctx.channel()
            )
            ctx.channel().attr<ServerSessionChannels>(AK_SERVER_SESSION_CHANNELS).set(sessionChannels)
            server.startTunnel(null, tunnelRequest.remotePort, sessionChannels)

            val head = Unpooled.buffer(9)
            head.writeBoolean(true)
            head.writeLong(tunnelId)
            ctx.channel().writeAndFlush(
                ProtoMessage(
                    ProtoCw.RESPONSE_OK,
                    tunnelId,
                    0,
                    tunnelRequest.toBytes()
                )
            )
            head.release()

        } catch (e: TunnelException) {
            val head = Unpooled.copyBoolean(false)
            ctx.channel().writeAndFlush(
                ProtoMessage(
                    ProtoCw.RESPONSE_ERR,
                    0,
                    0,
                    e.message.toString().toByteArray(StandardCharsets.UTF_8)
                )
            ).addListener(ChannelFutureListener.CLOSE)
            head.release()
        }

    }

    private fun handleHttpRequestMessage(ctx: ChannelHandlerContext, server: HttpServer, request: TunnelRequest) {
        try {
            val tunnelRequest = tunnelRequestInterceptor.handleTunnelRequest(request)
            if (server.registry.isRegistered(tunnelRequest.vhost)) {
                throw TunnelException("vhost(${tunnelRequest.vhost}) already used")
            }
            val tunnelId = tunnelIdProducer.nextId
            val sessionChannels = ServerSessionChannels(tunnelId, tunnelRequest, ctx.channel())
            ctx.channel().attr<ServerSessionChannels>(AK_SERVER_SESSION_CHANNELS).set(sessionChannels)
            server.registry.register(tunnelRequest.vhost, sessionChannels)

            ctx.channel().writeAndFlush(
                ProtoMessage(
                    ProtoCw.RESPONSE_OK,
                    tunnelId,
                    0,
                    tunnelRequest.toBytes()
                )
            )
        } catch (e: TunnelException) {
            val head = Unpooled.copyBoolean(false)
            ctx.channel().writeAndFlush(
                ProtoMessage(
                    ProtoCw.RESPONSE_ERR,
                    0,
                    0,
                    e.message.toString().toByteArray(StandardCharsets.UTF_8)
                )
            ).addListener(ChannelFutureListener.CLOSE)
            head.release()
        }

    }

}