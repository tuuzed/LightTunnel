@file:Suppress("UNUSED_PARAMETER")

package tpclient

import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import tpcommon.*
import java.nio.charset.StandardCharsets

class TPClientChannelHandler(
    private val localTcpClient: TPLocalTcpClient,
    private val onConnectStateListener: OnConnectStateListener
) : SimpleChannelInboundHandler<TPMassage>() {
    private val logger by logger()
    private val handler = InnerHandler()

    @Throws(Exception::class)
    override fun channelInactive(ctx: ChannelHandlerContext?) {
        // 隧道断开
        if (ctx != null) {
            val tpRequest = ctx.channel().attr(AK_TP_REQUEST).get()
            val tunnelId = ctx.channel().attr(AK_TUNNEL_ID).get()
            val sessionId = ctx.channel().attr(AK_SESSION_ID).get()
            when (tpRequest?.type) {
                TPType.TCP, TPType.HTTP, TPType.HTTPS -> {
                    if (tunnelId != null && sessionId != null) {
                        localTcpClient.removeLocalChannel(tunnelId, sessionId)?.close()
                    }
                }
                else -> {
                }
            }
            onConnectStateListener.onChannelInactive(ctx)
        }
        super.channelInactive(ctx)
    }

    @Throws(Exception::class)
    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
        logger.trace("exceptionCaught: {}", ctx, cause)
        ctx ?: return
        ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
    }

    @Throws(Exception::class)
    override fun channelRead0(ctx: ChannelHandlerContext?, msg: TPMassage?) {
        logger.trace("channelRead0 : {}, {}", ctx, msg)
        ctx ?: return
        msg ?: return
        when (msg.cmd) {
            TPCommand.PING -> handler.handlePingMessage(ctx, msg)
            TPCommand.RESPONSE_OK -> handler.handleResponseOkMessage(ctx, msg)
            TPCommand.RESPONSE_ERR -> handler.handleResponseErrMessage(ctx, msg)
            TPCommand.TRANSFER -> handler.handleTransferMessage(ctx, msg)
            TPCommand.REMOTE_CONNECTED -> handler.handleRemoteConnectedMessage(ctx, msg)
            TPCommand.REMOTE_DISCONNECT -> handler.handleRemoteDisconnectMessage(ctx, msg)
            else -> {
            }
        }
    }

    private inner class InnerHandler {

        /** Ping */
        @Throws(Exception::class)
        fun handlePingMessage(ctx: ChannelHandlerContext, msg: TPMassage) {
            ctx.writeAndFlush(TPMassage(TPCommand.PONG))
        }

        /** 隧道建立成功 */
        @Throws(Exception::class)
        fun handleResponseOkMessage(ctx: ChannelHandlerContext, msg: TPMassage) {
            val tunnelId = msg.headBuf.readLong()
            val tpRequest = TPRequest.fromBytes(msg.data)
            ctx.channel().attr(AK_TUNNEL_ID).set(tunnelId)
            ctx.channel().attr(AK_TP_REQUEST).set(tpRequest)
            ctx.channel().attr(AK_ERR_FLAG).set(null)
            ctx.channel().attr(AK_ERR_CAUSE).set(null)
            logger.debug("Opened Tunnel: {}", tpRequest)
            onConnectStateListener.onTunnelConnected(ctx)
        }

        /** 隧道建立失败 */
        @Throws(Exception::class)
        fun handleResponseErrMessage(ctx: ChannelHandlerContext, msg: TPMassage) {
            val errMessage = String(msg.head, StandardCharsets.UTF_8)
            ctx.channel().attr(AK_TUNNEL_ID).set(null)
            ctx.channel().attr(AK_TP_REQUEST).set(null)
            ctx.channel().attr(AK_ERR_FLAG).set(true)
            ctx.channel().attr(AK_ERR_CAUSE).set(Exception(errMessage))
            ctx.channel().close()
            logger.trace("Open Tunnel Error: {}", errMessage)
        }

        /** 数据透传消息 */
        @Throws(Exception::class)
        fun handleTransferMessage(ctx: ChannelHandlerContext, msg: TPMassage) {
            logger.trace("handleTransferMessage: msg: {}", msg)
            val tunnelId = msg.headBuf.readLong()
            val sessionId = msg.headBuf.readLong()
            ctx.channel().attr(AK_TUNNEL_ID).set(tunnelId)
            ctx.channel().attr(AK_SESSION_ID).set(sessionId)
            val tpRequest = ctx.channel().attr(AK_TP_REQUEST).get()
            if (tpRequest != null) {
                when (tpRequest.type) {
                    TPType.TCP, TPType.HTTP, TPType.HTTPS -> {
                        localTcpClient.getLocalChannel(
                            tpRequest.localAddr, tpRequest.localPort,
                            tunnelId, sessionId,
                            ctx.channel(),
                            object : TPLocalTcpClient.Callback {
                                override fun success(localChannel: Channel) {
                                    localChannel.writeAndFlush(Unpooled.wrappedBuffer(msg.data))
                                }
                            })
                    }
                    else -> {
                    }
                }
            }
        }

        /** 连接本地隧道消息 */
        @Throws(Exception::class)
        fun handleRemoteConnectedMessage(ctx: ChannelHandlerContext, msg: TPMassage) {
            val tunnelId = msg.headBuf.readLong()
            val sessionId = msg.headBuf.readLong()
            ctx.channel().attr(AK_TUNNEL_ID).set(tunnelId)
            ctx.channel().attr(AK_SESSION_ID).set(sessionId)
            val tpRequest = ctx.channel().attr(AK_TP_REQUEST).get()
            if (tpRequest != null) {
                localTcpClient.getLocalChannel(
                    tpRequest.localAddr, tpRequest.localPort,
                    tunnelId, sessionId,
                    ctx.channel(),
                    object : TPLocalTcpClient.Callback {}
                )
            }
        }

        /** 用户隧道断开消息 */
        @Throws(Exception::class)
        fun handleRemoteDisconnectMessage(ctx: ChannelHandlerContext, msg: TPMassage) {
            val tunnelId = msg.headBuf.readLong()
            val sessionId = msg.headBuf.readLong()
            localTcpClient.removeLocalChannel(tunnelId, sessionId)?.close()
        }
    }

}