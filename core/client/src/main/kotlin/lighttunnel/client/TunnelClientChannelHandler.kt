@file:Suppress("UNUSED_PARAMETER")

package lighttunnel.client

import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import lighttunnel.client.local.LocalTcpClient
import lighttunnel.client.util.AttrKeys
import lighttunnel.logging.loggerDelegate
import lighttunnel.proto.ProtoCommand
import lighttunnel.proto.ProtoMassage
import lighttunnel.proto.ProtoRequest
import java.nio.charset.StandardCharsets

class TunnelClientChannelHandler(
    private val localTcpClient: LocalTcpClient,
    private val onConnectStateListener: OnConnectStateListener
) : SimpleChannelInboundHandler<ProtoMassage>() {
    private val logger by loggerDelegate()
    private val handler = InnerHandler()

    @Throws(Exception::class)
    override fun channelInactive(ctx: ChannelHandlerContext?) {
        // 隧道断开
        if (ctx != null) {
            val request = ctx.channel().attr(AttrKeys.AK_LT_REQUEST).get()
            val tunnelId = ctx.channel().attr(AttrKeys.AK_TUNNEL_ID).get()
            val sessionId = ctx.channel().attr(AttrKeys.AK_SESSION_ID).get()
            when (request?.type) {
                ProtoRequest.Type.TCP, ProtoRequest.Type.HTTP, ProtoRequest.Type.HTTPS -> {
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
    override fun channelRead0(ctx: ChannelHandlerContext?, msg: ProtoMassage?) {
        logger.trace("channelRead0 : {}, {}", ctx, msg)
        ctx ?: return
        msg ?: return
        when (msg.cmd) {
            ProtoCommand.PING -> handler.handlePingMessage(ctx, msg)
            ProtoCommand.RESPONSE_OK -> handler.handleResponseOkMessage(ctx, msg)
            ProtoCommand.RESPONSE_ERR -> handler.handleResponseErrMessage(ctx, msg)
            ProtoCommand.TRANSFER -> handler.handleTransferMessage(ctx, msg)
            ProtoCommand.REMOTE_CONNECTED -> handler.handleRemoteConnectedMessage(ctx, msg)
            ProtoCommand.REMOTE_DISCONNECT -> handler.handleRemoteDisconnectMessage(ctx, msg)
            else -> {
            }
        }
    }

    private inner class InnerHandler {

        /** Ping */
        @Throws(Exception::class)
        fun handlePingMessage(ctx: ChannelHandlerContext, msg: ProtoMassage) {
            ctx.writeAndFlush(ProtoMassage(ProtoCommand.PONG))
        }

        /** 隧道建立成功 */
        @Throws(Exception::class)
        fun handleResponseOkMessage(ctx: ChannelHandlerContext, msg: ProtoMassage) {
            val tunnelId = msg.headBuf.readLong()
            val request = ProtoRequest.fromBytes(msg.data)
            ctx.channel().attr(AttrKeys.AK_TUNNEL_ID).set(tunnelId)
            ctx.channel().attr(AttrKeys.AK_LT_REQUEST).set(request)
            ctx.channel().attr(AttrKeys.AK_ERR_FLAG).set(null)
            ctx.channel().attr(AttrKeys.AK_ERR_CAUSE).set(null)
            logger.debug("Opened Tunnel: {}", request)
            onConnectStateListener.onTunnelConnected(ctx)
        }

        /** 隧道建立失败 */
        @Throws(Exception::class)
        fun handleResponseErrMessage(ctx: ChannelHandlerContext, msg: ProtoMassage) {
            val errMessage = String(msg.head, StandardCharsets.UTF_8)
            ctx.channel().attr(AttrKeys.AK_TUNNEL_ID).set(null)
            ctx.channel().attr(AttrKeys.AK_LT_REQUEST).set(null)
            ctx.channel().attr(AttrKeys.AK_ERR_FLAG).set(true)
            ctx.channel().attr(AttrKeys.AK_ERR_CAUSE).set(Exception(errMessage))
            ctx.channel().close()
            logger.trace("Open Tunnel Error: {}", errMessage)
        }

        /** 数据透传消息 */
        @Throws(Exception::class)
        fun handleTransferMessage(ctx: ChannelHandlerContext, msg: ProtoMassage) {
            logger.trace("handleTransferMessage: msg: {}", msg)
            val tunnelId = msg.headBuf.readLong()
            val sessionId = msg.headBuf.readLong()
            ctx.channel().attr(AttrKeys.AK_TUNNEL_ID).set(tunnelId)
            ctx.channel().attr(AttrKeys.AK_SESSION_ID).set(sessionId)
            val request = ctx.channel().attr(AttrKeys.AK_LT_REQUEST).get()
            if (request != null) {
                when (request.type) {
                    ProtoRequest.Type.TCP, ProtoRequest.Type.HTTP, ProtoRequest.Type.HTTPS -> {
                        localTcpClient.getLocalChannel(
                            request.localAddr, request.localPort,
                            tunnelId, sessionId,
                            ctx.channel(),
                            object : LocalTcpClient.Callback {
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
        fun handleRemoteConnectedMessage(ctx: ChannelHandlerContext, msg: ProtoMassage) {
            val tunnelId = msg.headBuf.readLong()
            val sessionId = msg.headBuf.readLong()
            ctx.channel().attr(AttrKeys.AK_TUNNEL_ID).set(tunnelId)
            ctx.channel().attr(AttrKeys.AK_SESSION_ID).set(sessionId)
            val tpRequest = ctx.channel().attr(AttrKeys.AK_LT_REQUEST).get()
            if (tpRequest != null) {
                localTcpClient.getLocalChannel(
                    tpRequest.localAddr, tpRequest.localPort,
                    tunnelId, sessionId,
                    ctx.channel(),
                    object : LocalTcpClient.Callback {}
                )
            }
        }

        /** 用户隧道断开消息 */
        @Throws(Exception::class)
        fun handleRemoteDisconnectMessage(ctx: ChannelHandlerContext, msg: ProtoMassage) {
            val tunnelId = msg.headBuf.readLong()
            val sessionId = msg.headBuf.readLong()
            localTcpClient.removeLocalChannel(tunnelId, sessionId)?.close()
        }
    }

}