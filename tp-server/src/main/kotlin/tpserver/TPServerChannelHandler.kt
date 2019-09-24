package tpserver

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import tpcommon.*

class TPServerChannelHandler(
    private val tpRequestInterceptor: TPRequestInterceptor,
    private val tunnelIds: TPIds,
    private val tcpServer: TPTcpServer? = null,
    private val httpServer: TPHttpServer? = null,
    private val httpsServer: TPHttpServer? = null
) : SimpleChannelInboundHandler<TPMassage>() {
    private val logger by logger()
    private val handler = InnerHandler()

    override fun channelInactive(ctx: ChannelHandlerContext?) {
        logger.trace("channelInactive: {}", ctx)
        if (ctx == null) {
            super.channelInactive(ctx)
            return
        }
        ctx.channel().attr(AK_TP_SESSION_POOL).get()?.also {
            when (it.tpRequest.type) {
                TPType.TCP -> tcpServer?.registry?.unregister(it.tunnelId)
                TPType.HTTP -> httpServer?.registry?.unregister(it.tpRequest.host)
                TPType.HTTPS -> httpsServer?.registry?.unregister(it.tpRequest.host)
                else -> {
                }
            }
        }
        ctx.channel().attr(AK_TP_SESSION_POOL).set(null)
        super.channelInactive(ctx)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
        ctx ?: return
        logger.trace("exceptionCaught: {}", ctx, cause)
        ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
    }

    override fun channelRead0(ctx: ChannelHandlerContext?, msg: TPMassage?) {
        ctx ?: return
        msg ?: return
        when (msg.cmd) {
            TPCommand.PING -> handler.handlePingMessage(ctx, msg)
            TPCommand.REQUEST -> handler.handleRequestMessage(ctx, msg)
            TPCommand.TRANSFER -> handler.handleTransferMessage(ctx, msg)
            TPCommand.LOCAL_CONNECTED -> handler.handleLocalConnectedMessage(ctx, msg)
            TPCommand.LOCAL_DISCONNECT -> handler.handleLocalDisconnectMessage(ctx, msg)
            else -> {
                // Nothing
            }
        }
    }

    private inner class InnerHandler {
        @Throws(Exception::class)
        fun handlePingMessage(ctx: ChannelHandlerContext, msg: TPMassage) {
            logger.trace("handlePingMessage# {}, {}", ctx, msg)
            ctx.writeAndFlush(TPMassage(TPCommand.PONG))
        }

        @Throws(Exception::class)
        fun handleRequestMessage(ctx: ChannelHandlerContext, msg: TPMassage) {
            logger.trace("handleRequestMessage# {}, {}", ctx, msg)
            try {
                val tpRequest = TPRequest.fromBytes(msg.head)
                logger.trace("tpRequest: {}", tpRequest)
                when (tpRequest.type) {
                    TPType.TCP -> {
                        tcpServer?.also { handleTcpRequestMessage(ctx, it, tpRequest) }
                            ?: throw TPException("TCP协议隧道未开启")
                    }
                    TPType.HTTP -> {
                        httpServer?.also { handleHttpRequestMessage(ctx, it, tpRequest) }
                            ?: throw TPException("HTTP协议隧道未开启")
                    }
                    TPType.HTTPS -> {
                        httpsServer?.also { handleHttpRequestMessage(ctx, it, tpRequest) }
                            ?: throw TPException("HTTPS协议隧道未开启")
                    }
                    else -> throw TPException("不支持的隧道类型")
                }
            } catch (e: Exception) {
                ctx.channel().writeAndFlush(
                    TPMassage(TPCommand.RESPONSE_ERR, e.message.toString().toByteArray())
                ).addListener(ChannelFutureListener.CLOSE)
            }
        }

        @Throws(Exception::class)
        fun handleTransferMessage(ctx: ChannelHandlerContext, msg: TPMassage) {
            val sessionPool = ctx.channel().attr(AK_TP_SESSION_POOL).get() ?: return
            when (sessionPool.tpRequest.type) {
                TPType.TCP -> {
                    tcpServer ?: return
                    val tunnelId = msg.headBuf.readLong()
                    val sessionId = msg.headBuf.readLong()
                    tcpServer.registry.getSessionChannel(tunnelId, sessionId)
                        ?.writeAndFlush(Unpooled.wrappedBuffer(msg.data))
                }
                TPType.HTTP -> {
                    httpServer ?: return
                    val tunnelId = msg.headBuf.readLong()
                    val sessionId = msg.headBuf.readLong()
                    httpServer.registry.getSessionChannel(tunnelId, sessionId)
                        ?.writeAndFlush(Unpooled.wrappedBuffer(msg.data))
                }
                TPType.HTTPS -> {
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
        fun handleLocalConnectedMessage(ctx: ChannelHandlerContext, msg: TPMassage) {
            logger.trace("handleLocalConnectedMessage# {}, {}", ctx, msg)
            // 无须处理
        }

        @Throws(Exception::class)
        fun handleLocalDisconnectMessage(ctx: ChannelHandlerContext, msg: TPMassage) {
            logger.trace("handleLocalDisconnectMessage# {}, {}", ctx, msg)
            val sessionPool = ctx.channel().attr(AK_TP_SESSION_POOL).get() ?: return
            val tunnelId = msg.headBuf.readLong()
            val sessionId = msg.headBuf.readLong()
            val sessionChannel = sessionPool.getChannel(sessionId)
            // 解决 HTTP/1.x 数据传输问题
            sessionChannel?.writeAndFlush(Unpooled.EMPTY_BUFFER)?.addListener(ChannelFutureListener.CLOSE)
        }

        @Throws(Exception::class)
        private fun handleTcpRequestMessage(ctx: ChannelHandlerContext, server: TPTcpServer, tpRequest: TPRequest) {
            val tunnelRequest = tpRequestInterceptor.handleTPRequest(tpRequest)
            val tunnelId = tunnelIds.nextId
            val sessionPool = TPSessionPool(tunnelId, tunnelRequest, ctx.channel())
            ctx.channel().attr(AK_TP_SESSION_POOL).set(sessionPool)
            server.startTunnel(null, tunnelRequest.remotePort, sessionPool)
            val head = ctx.alloc().long2Bytes(tunnelId, 0)
            val data = tunnelRequest.toBytes()
            ctx.channel().writeAndFlush(TPMassage(TPCommand.RESPONSE_OK, head, data))
        }

        @Throws(Exception::class)
        private fun handleHttpRequestMessage(ctx: ChannelHandlerContext, server: TPHttpServer, tpRequest: TPRequest) {
            val request = tpRequestInterceptor.handleTPRequest(tpRequest)
            if (server.registry.isRegistered(request.host)) {
                throw TPException("host(${request.host}) already used")
            }
            val tunnelId = tunnelIds.nextId
            val sessionPool = TPSessionPool(tunnelId, request, ctx.channel())
            ctx.channel().attr(AK_TP_SESSION_POOL).set(sessionPool)
            server.registry.register(request.host, sessionPool)
            val head = ctx.alloc().long2Bytes(tunnelId, 0)
            val data = request.toBytes()
            ctx.channel().writeAndFlush(TPMassage(TPCommand.RESPONSE_OK, head, data))
        }
    }

}