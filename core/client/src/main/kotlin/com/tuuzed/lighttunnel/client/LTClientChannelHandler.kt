@file:Suppress("UNUSED_PARAMETER")

package com.tuuzed.lighttunnel.client

import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import com.tuuzed.lighttunnel.common.*
import java.nio.charset.StandardCharsets

class LTClientChannelHandler(
    private val localTcpClient: LTLocalTcpClient,
    private val onConnectStateListener: OnConnectStateListener
) : SimpleChannelInboundHandler<LTMassage>() {
    private val logger by logger()
    private val handler = InnerHandler()

    @Throws(Exception::class)
    override fun channelInactive(ctx: ChannelHandlerContext?) {
        // 隧道断开
        if (ctx != null) {
            val request = ctx.channel().attr(AK_LT_REQUEST).get()
            val tunnelId = ctx.channel().attr(AK_TUNNEL_ID).get()
            val sessionId = ctx.channel().attr(AK_SESSION_ID).get()
            when (request?.type) {
                LTRequest.Type.TCP, LTRequest.Type.HTTP, LTRequest.Type.HTTPS -> {
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
    override fun channelRead0(ctx: ChannelHandlerContext?, msg: LTMassage?) {
        logger.trace("channelRead0 : {}, {}", ctx, msg)
        ctx ?: return
        msg ?: return
        when (msg.cmd) {
            LTCommand.PING -> handler.handlePingMessage(ctx, msg)
            LTCommand.RESPONSE_OK -> handler.handleResponseOkMessage(ctx, msg)
            LTCommand.RESPONSE_ERR -> handler.handleResponseErrMessage(ctx, msg)
            LTCommand.TRANSFER -> handler.handleTransferMessage(ctx, msg)
            LTCommand.REMOTE_CONNECTED -> handler.handleRemoteConnectedMessage(ctx, msg)
            LTCommand.REMOTE_DISCONNECT -> handler.handleRemoteDisconnectMessage(ctx, msg)
            else -> {
            }
        }
    }

    private inner class InnerHandler {

        /** Ping */
        @Throws(Exception::class)
        fun handlePingMessage(ctx: ChannelHandlerContext, msg: LTMassage) {
            ctx.writeAndFlush(LTMassage(LTCommand.PONG))
        }

        /** 隧道建立成功 */
        @Throws(Exception::class)
        fun handleResponseOkMessage(ctx: ChannelHandlerContext, msg: LTMassage) {
            val tunnelId = msg.headBuf.readLong()
            val request = LTRequest.fromBytes(msg.data)
            ctx.channel().attr(AK_TUNNEL_ID).set(tunnelId)
            ctx.channel().attr(AK_LT_REQUEST).set(request)
            ctx.channel().attr(AK_ERR_FLAG).set(null)
            ctx.channel().attr(AK_ERR_CAUSE).set(null)
            logger.debug("Opened Tunnel: {}", request)
            onConnectStateListener.onTunnelConnected(ctx)
        }

        /** 隧道建立失败 */
        @Throws(Exception::class)
        fun handleResponseErrMessage(ctx: ChannelHandlerContext, msg: LTMassage) {
            val errMessage = String(msg.head, StandardCharsets.UTF_8)
            ctx.channel().attr(AK_TUNNEL_ID).set(null)
            ctx.channel().attr(AK_LT_REQUEST).set(null)
            ctx.channel().attr(AK_ERR_FLAG).set(true)
            ctx.channel().attr(AK_ERR_CAUSE).set(Exception(errMessage))
            ctx.channel().close()
            logger.trace("Open Tunnel Error: {}", errMessage)
        }

        /** 数据透传消息 */
        @Throws(Exception::class)
        fun handleTransferMessage(ctx: ChannelHandlerContext, msg: LTMassage) {
            logger.trace("handleTransferMessage: msg: {}", msg)
            val tunnelId = msg.headBuf.readLong()
            val sessionId = msg.headBuf.readLong()
            ctx.channel().attr(AK_TUNNEL_ID).set(tunnelId)
            ctx.channel().attr(AK_SESSION_ID).set(sessionId)
            val request = ctx.channel().attr(AK_LT_REQUEST).get()
            if (request != null) {
                when (request.type) {
                    LTRequest.Type.TCP, LTRequest.Type.HTTP, LTRequest.Type.HTTPS -> {
                        localTcpClient.getLocalChannel(
                            request.localAddr, request.localPort,
                            tunnelId, sessionId,
                            ctx.channel(),
                            object : LTLocalTcpClient.Callback {
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
        fun handleRemoteConnectedMessage(ctx: ChannelHandlerContext, msg: LTMassage) {
            val tunnelId = msg.headBuf.readLong()
            val sessionId = msg.headBuf.readLong()
            ctx.channel().attr(AK_TUNNEL_ID).set(tunnelId)
            ctx.channel().attr(AK_SESSION_ID).set(sessionId)
            val tpRequest = ctx.channel().attr(AK_LT_REQUEST).get()
            if (tpRequest != null) {
                localTcpClient.getLocalChannel(
                    tpRequest.localAddr, tpRequest.localPort,
                    tunnelId, sessionId,
                    ctx.channel(),
                    object : LTLocalTcpClient.Callback {}
                )
            }
        }

        /** 用户隧道断开消息 */
        @Throws(Exception::class)
        fun handleRemoteDisconnectMessage(ctx: ChannelHandlerContext, msg: LTMassage) {
            val tunnelId = msg.headBuf.readLong()
            val sessionId = msg.headBuf.readLong()
            localTcpClient.removeLocalChannel(tunnelId, sessionId)?.close()
        }
    }

}