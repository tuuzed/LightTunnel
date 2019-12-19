package lighttunnel.server

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import lighttunnel.logging.logger
import lighttunnel.proto.LTCommand
import lighttunnel.proto.LTException
import lighttunnel.proto.LTMassage
import lighttunnel.proto.LTRequest
import lighttunnel.util.long2Bytes

class LTServerChannelHandler(
    private val tpRequestInterceptor: LTRequestInterceptor,
    private val tunnelIds: LTIncIds,
    private val tcpServer: LTTcpServer? = null,
    private val httpServer: LTHttpServer? = null,
    private val httpsServer: LTHttpServer? = null
) : SimpleChannelInboundHandler<LTMassage>() {
    private val logger by logger()
    private val handler = InnerHandler()

    override fun channelInactive(ctx: ChannelHandlerContext?) {
        logger.trace("channelInactive: {}", ctx)
        if (ctx == null) {
            super.channelInactive(ctx)
            return
        }
        ctx.channel().attr(AK_SESSION_POOL).get()?.also {
            when (it.request.type) {
                LTRequest.Type.TCP -> tcpServer?.registry?.unregister(it.tunnelId)
                LTRequest.Type.HTTP -> httpServer?.registry?.unregister(it.request.host)
                LTRequest.Type.HTTPS -> httpsServer?.registry?.unregister(it.request.host)
                else -> {
                }
            }
        }
        ctx.channel().attr(AK_SESSION_POOL).set(null)
        super.channelInactive(ctx)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
        ctx ?: return
        logger.trace("exceptionCaught: {}", ctx, cause)
        ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
    }

    override fun channelRead0(ctx: ChannelHandlerContext?, msg: LTMassage?) {
        ctx ?: return
        msg ?: return
        when (msg.cmd) {
            LTCommand.PING -> handler.handlePingMessage(ctx, msg)
            LTCommand.REQUEST -> handler.handleRequestMessage(ctx, msg)
            LTCommand.TRANSFER -> handler.handleTransferMessage(ctx, msg)
            LTCommand.LOCAL_CONNECTED -> handler.handleLocalConnectedMessage(ctx, msg)
            LTCommand.LOCAL_DISCONNECT -> handler.handleLocalDisconnectMessage(ctx, msg)
            else -> {
                // Nothing
            }
        }
    }

    private inner class InnerHandler {
        @Throws(Exception::class)
        fun handlePingMessage(ctx: ChannelHandlerContext, msg: LTMassage) {
            logger.trace("handlePingMessage# {}, {}", ctx, msg)
            ctx.writeAndFlush(LTMassage(LTCommand.PONG))
        }

        @Throws(Exception::class)
        fun handleRequestMessage(ctx: ChannelHandlerContext, msg: LTMassage) {
            logger.trace("handleRequestMessage# {}, {}", ctx, msg)
            try {
                val tpRequest = LTRequest.fromBytes(msg.head)
                logger.trace("tpRequest: {}", tpRequest)
                when (tpRequest.type) {
                    LTRequest.Type.TCP -> {
                        tcpServer?.also { handleTcpRequestMessage(ctx, it, tpRequest) }
                            ?: throw LTException("TCP协议隧道未开启")
                    }
                    LTRequest.Type.HTTP -> {
                        httpServer?.also { handleHttpRequestMessage(ctx, it, tpRequest) }
                            ?: throw LTException("HTTP协议隧道未开启")
                    }
                    LTRequest.Type.HTTPS -> {
                        httpsServer?.also { handleHttpRequestMessage(ctx, it, tpRequest) }
                            ?: throw LTException("HTTPS协议隧道未开启")
                    }
                    else -> throw LTException("不支持的隧道类型")
                }
            } catch (e: Exception) {
                ctx.channel().writeAndFlush(
                    LTMassage(LTCommand.RESPONSE_ERR, e.message.toString().toByteArray())
                ).addListener(ChannelFutureListener.CLOSE)
            }
        }

        @Throws(Exception::class)
        fun handleTransferMessage(ctx: ChannelHandlerContext, msg: LTMassage) {
            val sessionPool = ctx.channel().attr(AK_SESSION_POOL).get() ?: return
            when (sessionPool.request.type) {
                LTRequest.Type.TCP -> {
                    tcpServer ?: return
                    val tunnelId = msg.headBuf.readLong()
                    val sessionId = msg.headBuf.readLong()
                    tcpServer.registry.getSessionChannel(tunnelId, sessionId)
                        ?.writeAndFlush(Unpooled.wrappedBuffer(msg.data))
                }
                LTRequest.Type.HTTP -> {
                    httpServer ?: return
                    val tunnelId = msg.headBuf.readLong()
                    val sessionId = msg.headBuf.readLong()
                    httpServer.registry.getSessionChannel(tunnelId, sessionId)
                        ?.writeAndFlush(Unpooled.wrappedBuffer(msg.data))
                }
                LTRequest.Type.HTTPS -> {
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
        fun handleLocalConnectedMessage(ctx: ChannelHandlerContext, msg: LTMassage) {
            logger.trace("handleLocalConnectedMessage# {}, {}", ctx, msg)
            // 无须处理
        }

        @Suppress("UNUSED_VARIABLE")
        @Throws(Exception::class)
        fun handleLocalDisconnectMessage(ctx: ChannelHandlerContext, msg: LTMassage) {
            logger.trace("handleLocalDisconnectMessage# {}, {}", ctx, msg)
            val sessionPool = ctx.channel().attr(AK_SESSION_POOL).get() ?: return
            val tunnelId = msg.headBuf.readLong()
            val sessionId = msg.headBuf.readLong()
            val sessionChannel = sessionPool.getChannel(sessionId)
            // 解决 HTTP/1.x 数据传输问题
            sessionChannel?.writeAndFlush(Unpooled.EMPTY_BUFFER)?.addListener(ChannelFutureListener.CLOSE)
        }

        @Throws(Exception::class)
        private fun handleTcpRequestMessage(ctx: ChannelHandlerContext, server: LTTcpServer, tpRequest: LTRequest) {
            val tunnelRequest = tpRequestInterceptor.handleTPRequest(tpRequest)
            val tunnelId = tunnelIds.nextId
            val sessionPool = LTSessionPool(tunnelId, tunnelRequest, ctx.channel())
            ctx.channel().attr(AK_SESSION_POOL).set(sessionPool)
            server.startTunnel(null, tunnelRequest.remotePort, sessionPool)
            val head = ctx.alloc().long2Bytes(tunnelId, 0)
            val data = tunnelRequest.toBytes()
            ctx.channel().writeAndFlush(LTMassage(LTCommand.RESPONSE_OK, head, data))
        }

        @Throws(Exception::class)
        private fun handleHttpRequestMessage(ctx: ChannelHandlerContext, server: LTHttpServer, tpRequest: LTRequest) {
            val request = tpRequestInterceptor.handleTPRequest(tpRequest)
            if (server.registry.isRegistered(request.host)) {
                throw LTException("host(${request.host}) already used")
            }
            val tunnelId = tunnelIds.nextId
            val sessionPool = LTSessionPool(tunnelId, request, ctx.channel())
            ctx.channel().attr(AK_SESSION_POOL).set(sessionPool)
            server.registry.register(request.host, sessionPool)
            val head = ctx.alloc().long2Bytes(tunnelId, 0)
            val data = request.toBytes()
            ctx.channel().writeAndFlush(LTMassage(LTCommand.RESPONSE_OK, head, data))
        }
    }

}