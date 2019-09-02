package tunnel2.client

import io.netty.buffer.Unpooled
import io.netty.channel.*
import tunnel2.client.internal.*
import tunnel2.client.local.LocalConnector
import tunnel2.common.TunnelRequest
import tunnel2.common.logging.LoggerFactory
import tunnel2.common.proto.ProtoCw
import tunnel2.common.proto.ProtoMessage
import java.nio.charset.StandardCharsets

@ChannelHandler.Sharable
class TunnelClientChannelHandler(
    private val localConnector: LocalConnector,
    private val listener: Listener
) : SimpleChannelInboundHandler<ProtoMessage>() {

    companion object {
        private val logger = LoggerFactory.getLogger(TunnelClientChannelHandler::class.java)
    }

    @Throws(Exception::class)
    override fun channelInactive(ctx: ChannelHandlerContext?) {
        ctx ?: return
        // 隧道断开
        val tunnelId = ctx.channel().attr<Long>(AK_TUNNEL_ID).get()
        val sessionId = ctx.channel().attr<Long>(AK_SESSION_ID).get()
        if (tunnelId != null && sessionId != null) {
            localConnector.removeLocalChannel(tunnelId, sessionId)?.close()
        }
        super.channelInactive(ctx)
        listener.channelInactive(ctx)
    }

    @Throws(Exception::class)
    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
        logger.trace("exceptionCaught: {}", ctx, cause)
        ctx ?: return
        ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
    }

    override fun channelRead0(ctx: ChannelHandlerContext?, msg: ProtoMessage?) {
        logger.trace("channelRead0 : {}, {}", ctx, msg)
        ctx ?: return
        msg ?: return

        when (msg.cw) {
            ProtoCw.PING -> handlePingMessage(ctx, msg)
            ProtoCw.RESPONSE_OK -> handleResponseOkMessage(ctx, msg)
            ProtoCw.RESPONSE_ERR -> handleResponseErrMessage(ctx, msg)
            ProtoCw.TRANSFER -> handleTransferMessage(ctx, msg)
            ProtoCw.REMOTE_CONNECTED -> handleRemoteConnectedMessage(ctx, msg)
            ProtoCw.REMOTE_DISCONNECT -> handleRemoteDisconnectMessage(ctx, msg)
            else -> {
                // pass
            }
        }
    }

    @Throws(Exception::class)
    private fun handlePingMessage(ctx: ChannelHandlerContext, msg: ProtoMessage) {
        ctx.writeAndFlush(ProtoMessage(ProtoCw.PONG))
    }

    @Throws(Exception::class)
    private fun handleResponseOkMessage(ctx: ChannelHandlerContext, msg: ProtoMessage) {
        val tunnelRequest = TunnelRequest.fromBytes(msg.data)
        ctx.channel().attr(AK_TUNNEL_ID).set(msg.tunnelId)
        ctx.channel().attr(AK_TUNNEL_REQUEST).set(tunnelRequest)
        ctx.channel().attr(AK_ERR_FLAG).set(null)
        ctx.channel().attr(AK_ERR_CAUSE).set(null)
        logger.debug("Opened Tunnel: {}", tunnelRequest)
        listener.tunnelConnected(ctx)
    }

    @Throws(Exception::class)
    private fun handleResponseErrMessage(ctx: ChannelHandlerContext, msg: ProtoMessage) {
        val errMessage = String(msg.data, StandardCharsets.UTF_8)
        ctx.channel().attr(AK_TUNNEL_ID).set(null)
        ctx.channel().attr(AK_TUNNEL_REQUEST).set(null)
        ctx.channel().attr(AK_ERR_FLAG).set(true)
        ctx.channel().attr(AK_ERR_CAUSE).set(Exception(errMessage))
        ctx.channel().close()
        logger.trace("Open Tunnel Error: {}", errMessage)
    }


    /**
     * 处理数据透传消息
     */
    @Throws(Exception::class)
    private fun handleTransferMessage(ctx: ChannelHandlerContext, msg: ProtoMessage) {
        logger.trace("handleTransferMessage: msg: {}", msg)
        ctx.channel().attr(AK_TUNNEL_ID).set(msg.tunnelId)
        ctx.channel().attr(AK_SESSION_ID).set(msg.sessionId)
        val tunnelRequest = ctx.channel().attr<TunnelRequest>(AK_TUNNEL_REQUEST).get()
        if (tunnelRequest != null) {
            localConnector.acquireLocalChannel(
                tunnelRequest.localAddr, tunnelRequest.localPort,
                msg.tunnelId, msg.sessionId,
                ctx.channel(), object : LocalConnector.Callback {
                override fun success(localChannel: Channel) {
                    localChannel.writeAndFlush(Unpooled.wrappedBuffer(msg.data))
                }

                override fun error(cause: Throwable) {}
            })
        }
    }

    /**
     * 处理连接本地隧道消息
     */
    @Throws(Exception::class)
    private fun handleRemoteConnectedMessage(ctx: ChannelHandlerContext, msg: ProtoMessage) {
        ctx.channel().attr(AK_TUNNEL_ID).set(msg.tunnelId)
        ctx.channel().attr(AK_SESSION_ID).set(msg.sessionId)
        val tunnelRequest = ctx.channel().attr<TunnelRequest>(AK_TUNNEL_REQUEST).get()
        if (tunnelRequest != null) {
            localConnector.acquireLocalChannel(
                tunnelRequest.localAddr, tunnelRequest.localPort,
                msg.tunnelId, msg.sessionId,
                ctx.channel(), object : LocalConnector.Callback {})
        }
    }

    /**
     * 处理用户隧道断开消息
     */
    @Throws(Exception::class)
    private fun handleRemoteDisconnectMessage(ctx: ChannelHandlerContext, msg: ProtoMessage) {
        localConnector.removeLocalChannel(msg.tunnelId, msg.sessionId)?.close()
    }


    interface Listener {

        fun channelInactive(ctx: ChannelHandlerContext) {}

        fun tunnelConnected(ctx: ChannelHandlerContext) {}

    }


}