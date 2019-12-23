@file:Suppress("UNUSED_PARAMETER")

package lighttunnel.client

import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import lighttunnel.client.local.LocalTcpClient
import lighttunnel.client.util.AttributeKeys
import lighttunnel.logger.loggerDelegate
import lighttunnel.proto.ProtoCommand
import lighttunnel.proto.ProtoMessage
import lighttunnel.proto.TunnelRequest
import java.nio.charset.StandardCharsets

class TunnelClientChannelHandler(
    private val localTcpClient: LocalTcpClient,
    private val connectStateListener: OnConnectStateListener
) : SimpleChannelInboundHandler<ProtoMessage>() {
    private val logger by loggerDelegate()

    @Throws(Exception::class)
    override fun channelInactive(ctx: ChannelHandlerContext?) {
        // 隧道断开
        if (ctx != null) {
            val request = ctx.channel().attr(AttributeKeys.AK_TUNNEL_REQUEST).get()
            val tunnelId = ctx.channel().attr(AttributeKeys.AK_TUNNEL_ID).get()
            val sessionId = ctx.channel().attr(AttributeKeys.AK_SESSION_ID).get()
            when (request?.type) {
                TunnelRequest.Type.TCP, TunnelRequest.Type.HTTP, TunnelRequest.Type.HTTPS -> {
                    if (tunnelId != null && sessionId != null) {
                        localTcpClient.removeLocalChannel(tunnelId, sessionId)?.close()
                    }
                }
                else -> {
                }
            }
            connectStateListener.onChannelInactive(ctx)
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
    override fun channelRead0(ctx: ChannelHandlerContext?, msg: ProtoMessage?) {
        logger.trace("channelRead0 : {}, {}", ctx, msg)
        ctx ?: return
        msg ?: return
        when (msg.cmd) {
            ProtoCommand.PING -> doHandlePingMessage(ctx, msg)
            ProtoCommand.RESPONSE_OK -> doHandleResponseOkMessage(ctx, msg)
            ProtoCommand.RESPONSE_ERR -> doHandleResponseErrMessage(ctx, msg)
            ProtoCommand.TRANSFER -> doHandleTransferMessage(ctx, msg)
            ProtoCommand.REMOTE_CONNECTED -> doHandleRemoteConnectedMessage(ctx, msg)
            ProtoCommand.REMOTE_DISCONNECT -> doHandleRemoteDisconnectMessage(ctx, msg)
            else -> {
            }
        }
    }


    /** Ping */
    @Throws(Exception::class)
    private fun doHandlePingMessage(ctx: ChannelHandlerContext, msg: ProtoMessage) {
        ctx.writeAndFlush(ProtoMessage(ProtoCommand.PONG))
    }

    /** 隧道建立成功 */
    @Throws(Exception::class)
    private fun doHandleResponseOkMessage(ctx: ChannelHandlerContext, msg: ProtoMessage) {
        val request = TunnelRequest.fromBytes(msg.data)
        ctx.channel().attr(AttributeKeys.AK_TUNNEL_ID).set(msg.tunnelId)
        ctx.channel().attr(AttributeKeys.AK_TUNNEL_REQUEST).set(request)
        ctx.channel().attr(AttributeKeys.AK_ERR_FLAG).set(null)
        ctx.channel().attr(AttributeKeys.AK_ERR_CAUSE).set(null)
        logger.debug("Opened Tunnel: {}", request)
        connectStateListener.onTunnelConnected(ctx)
    }

    /** 隧道建立失败 */
    @Throws(Exception::class)
    private fun doHandleResponseErrMessage(ctx: ChannelHandlerContext, msg: ProtoMessage) {
        val errMessage = String(msg.head, StandardCharsets.UTF_8)
        ctx.channel().attr(AttributeKeys.AK_TUNNEL_ID).set(null)
        ctx.channel().attr(AttributeKeys.AK_TUNNEL_REQUEST).set(null)
        ctx.channel().attr(AttributeKeys.AK_ERR_FLAG).set(true)
        ctx.channel().attr(AttributeKeys.AK_ERR_CAUSE).set(Exception(errMessage))
        ctx.channel().close()
        logger.trace("Open Tunnel Error: {}", errMessage)
    }

    /** 数据透传消息 */
    @Throws(Exception::class)
    private fun doHandleTransferMessage(ctx: ChannelHandlerContext, msg: ProtoMessage) {
        logger.trace("handleTransferMessage: msg: {}", msg)
        ctx.channel().attr(AttributeKeys.AK_TUNNEL_ID).set(msg.tunnelId)
        ctx.channel().attr(AttributeKeys.AK_SESSION_ID).set(msg.sessionId)
        val request = ctx.channel().attr(AttributeKeys.AK_TUNNEL_REQUEST).get()
        if (request != null) {
            when (request.type) {
                TunnelRequest.Type.TCP, TunnelRequest.Type.HTTP, TunnelRequest.Type.HTTPS -> {
                    localTcpClient.getLocalChannel(
                        request.localAddr, request.localPort,
                        msg.tunnelId, msg.sessionId,
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
    private fun doHandleRemoteConnectedMessage(ctx: ChannelHandlerContext, msg: ProtoMessage) {
        ctx.channel().attr(AttributeKeys.AK_TUNNEL_ID).set(msg.tunnelId)
        ctx.channel().attr(AttributeKeys.AK_SESSION_ID).set(msg.sessionId)
        val tpRequest = ctx.channel().attr(AttributeKeys.AK_TUNNEL_REQUEST).get()
        if (tpRequest != null) {
            localTcpClient.getLocalChannel(
                tpRequest.localAddr, tpRequest.localPort,
                msg.tunnelId, msg.sessionId,
                ctx.channel(),
                object : LocalTcpClient.Callback {}
            )
        }
    }

    /** 用户隧道断开消息 */
    @Throws(Exception::class)
    private fun doHandleRemoteDisconnectMessage(ctx: ChannelHandlerContext, msg: ProtoMessage) {
        localTcpClient.removeLocalChannel(msg.tunnelId, msg.sessionId)?.close()
    }

}