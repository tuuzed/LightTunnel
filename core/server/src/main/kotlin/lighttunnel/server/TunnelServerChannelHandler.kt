package lighttunnel.server

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import lighttunnel.logging.loggerDelegate
import lighttunnel.proto.ProtoCommand
import lighttunnel.proto.ProtoException
import lighttunnel.proto.ProtoMassage
import lighttunnel.proto.ProtoRequest
import lighttunnel.server.http.HttpServer
import lighttunnel.server.interceptor.RequestInterceptor
import lighttunnel.server.tcp.TcpServer
import lighttunnel.server.util.AttrKeys
import lighttunnel.server.util.IncId
import lighttunnel.util.long2Bytes

class TunnelServerChannelHandler(
    private val tpRequestInterceptor: RequestInterceptor,
    private val tunnelIds: IncId,
    private val tcpServer: TcpServer? = null,
    private val httpServer: HttpServer? = null,
    private val httpsServer: HttpServer? = null
) : SimpleChannelInboundHandler<ProtoMassage>() {
    private val logger by loggerDelegate()
    private val handler = InnerHandler()

    override fun channelInactive(ctx: ChannelHandlerContext?) {
        logger.trace("channelInactive: {}", ctx)
        if (ctx == null) {
            super.channelInactive(ctx)
            return
        }
        ctx.channel().attr(AttrKeys.AK_SESSION_POOL).get()?.also {
            when (it.request.type) {
                ProtoRequest.Type.TCP -> tcpServer?.registry?.unregister(it.tunnelId)
                ProtoRequest.Type.HTTP -> httpServer?.registry?.unregister(it.request.host)
                ProtoRequest.Type.HTTPS -> httpsServer?.registry?.unregister(it.request.host)
                else -> {
                }
            }
        }
        ctx.channel().attr(AttrKeys.AK_SESSION_POOL).set(null)
        super.channelInactive(ctx)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
        ctx ?: return
        logger.trace("exceptionCaught: {}", ctx, cause)
        ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
    }

    override fun channelRead0(ctx: ChannelHandlerContext?, msg: ProtoMassage?) {
        ctx ?: return
        msg ?: return
        when (msg.cmd) {
            ProtoCommand.PING -> handler.handlePingMessage(ctx, msg)
            ProtoCommand.REQUEST -> handler.handleRequestMessage(ctx, msg)
            ProtoCommand.TRANSFER -> handler.handleTransferMessage(ctx, msg)
            ProtoCommand.LOCAL_CONNECTED -> handler.handleLocalConnectedMessage(ctx, msg)
            ProtoCommand.LOCAL_DISCONNECT -> handler.handleLocalDisconnectMessage(ctx, msg)
            else -> {
                // Nothing
            }
        }
    }

    private inner class InnerHandler {
        @Throws(Exception::class)
        fun handlePingMessage(ctx: ChannelHandlerContext, msg: ProtoMassage) {
            logger.trace("handlePingMessage# {}, {}", ctx, msg)
            ctx.writeAndFlush(ProtoMassage(ProtoCommand.PONG))
        }

        @Throws(Exception::class)
        fun handleRequestMessage(ctx: ChannelHandlerContext, msg: ProtoMassage) {
            logger.trace("handleRequestMessage# {}, {}", ctx, msg)
            try {
                val tpRequest = ProtoRequest.fromBytes(msg.head)
                logger.trace("tpRequest: {}", tpRequest)
                when (tpRequest.type) {
                    ProtoRequest.Type.TCP -> {
                        tcpServer?.also { handleTcpRequestMessage(ctx, it, tpRequest) }
                            ?: throw ProtoException("TCP协议隧道未开启")
                    }
                    ProtoRequest.Type.HTTP -> {
                        httpServer?.also { handleHttpRequestMessage(ctx, it, tpRequest) }
                            ?: throw ProtoException("HTTP协议隧道未开启")
                    }
                    ProtoRequest.Type.HTTPS -> {
                        httpsServer?.also { handleHttpRequestMessage(ctx, it, tpRequest) }
                            ?: throw ProtoException("HTTPS协议隧道未开启")
                    }
                    else -> throw ProtoException("不支持的隧道类型")
                }
            } catch (e: Exception) {
                ctx.channel().writeAndFlush(
                    ProtoMassage(ProtoCommand.RESPONSE_ERR, e.message.toString().toByteArray())
                ).addListener(ChannelFutureListener.CLOSE)
            }
        }

        @Throws(Exception::class)
        fun handleTransferMessage(ctx: ChannelHandlerContext, msg: ProtoMassage) {
            val sessionPool = ctx.channel().attr(AttrKeys.AK_SESSION_POOL).get() ?: return
            when (sessionPool.request.type) {
                ProtoRequest.Type.TCP -> {
                    tcpServer ?: return
                    val tunnelId = msg.headBuf.readLong()
                    val sessionId = msg.headBuf.readLong()
                    tcpServer.registry.getSessionChannel(tunnelId, sessionId)
                        ?.writeAndFlush(Unpooled.wrappedBuffer(msg.data))
                }
                ProtoRequest.Type.HTTP -> {
                    httpServer ?: return
                    val tunnelId = msg.headBuf.readLong()
                    val sessionId = msg.headBuf.readLong()
                    httpServer.registry.getSessionChannel(tunnelId, sessionId)
                        ?.writeAndFlush(Unpooled.wrappedBuffer(msg.data))
                }
                ProtoRequest.Type.HTTPS -> {
                    httpsServer ?: return
                    val tunnelId = msg.headBuf.readLong()
                    val sessionId = msg.headBuf.readLong()
                    httpsServer.registry.getSessionChannel(tunnelId, sessionId)
                        ?.writeAndFlush(Unpooled.wrappedBuffer(msg.data))
                }
                else -> {
                }
            }
        }

        @Throws(Exception::class)
        fun handleLocalConnectedMessage(ctx: ChannelHandlerContext, msg: ProtoMassage) {
            logger.trace("handleLocalConnectedMessage# {}, {}", ctx, msg)
            // 无须处理
        }

        @Suppress("UNUSED_VARIABLE")
        @Throws(Exception::class)
        fun handleLocalDisconnectMessage(ctx: ChannelHandlerContext, msg: ProtoMassage) {
            logger.trace("handleLocalDisconnectMessage# {}, {}", ctx, msg)
            val sessionPool = ctx.channel().attr(AttrKeys.AK_SESSION_POOL).get() ?: return
            val tunnelId = msg.headBuf.readLong()
            val sessionId = msg.headBuf.readLong()
            val sessionChannel = sessionPool.getChannel(sessionId)
            // 解决 HTTP/1.x 数据传输问题
            sessionChannel?.writeAndFlush(Unpooled.EMPTY_BUFFER)?.addListener(ChannelFutureListener.CLOSE)
        }

        @Throws(Exception::class)
        private fun handleTcpRequestMessage(ctx: ChannelHandlerContext, server: TcpServer, tpRequest: ProtoRequest) {
            val tunnelRequest = tpRequestInterceptor.handleTPRequest(tpRequest)
            val tunnelId = tunnelIds.nextId
            val sessionPool = SessionPool(tunnelId, tunnelRequest, ctx.channel())
            ctx.channel().attr(AttrKeys.AK_SESSION_POOL).set(sessionPool)
            server.startTunnel(null, tunnelRequest.remotePort, sessionPool)
            val head = ctx.alloc().long2Bytes(tunnelId, 0)
            val data = tunnelRequest.toBytes()
            ctx.channel().writeAndFlush(ProtoMassage(ProtoCommand.RESPONSE_OK, head, data))
        }

        @Throws(Exception::class)
        private fun handleHttpRequestMessage(ctx: ChannelHandlerContext, server: HttpServer, tpRequest: ProtoRequest) {
            val request = tpRequestInterceptor.handleTPRequest(tpRequest)
            if (server.registry.isRegistered(request.host)) {
                throw ProtoException("host(${request.host}) already used")
            }
            val tunnelId = tunnelIds.nextId
            val sessionPool = SessionPool(tunnelId, request, ctx.channel())
            ctx.channel().attr(AttrKeys.AK_SESSION_POOL).set(sessionPool)
            server.registry.register(request.host, sessionPool)
            val head = ctx.alloc().long2Bytes(tunnelId, 0)
            val data = request.toBytes()
            ctx.channel().writeAndFlush(ProtoMassage(ProtoCommand.RESPONSE_OK, head, data))
        }
    }

}