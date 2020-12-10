package lighttunnel.internal.client

import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import lighttunnel.RemoteConnection
import lighttunnel.TunnelRequest
import lighttunnel.TunnelType
import lighttunnel.internal.base.proto.ProtoMessage
import lighttunnel.internal.base.proto.ProtoMessageType
import lighttunnel.internal.base.proto.message.*
import lighttunnel.internal.base.util.loggerDelegate
import lighttunnel.internal.client.conn.TunnelConnectionDefaultImpl
import lighttunnel.internal.client.local.LocalTcpClient
import lighttunnel.internal.client.util.*
import lighttunnel.listener.OnRemoteConnectionListener
import java.nio.charset.StandardCharsets

internal class TunnelClientDaemonChannelHandler(
    private val localTcpClient: LocalTcpClient,
    private val onChannelStateListener: OnChannelStateListener,
    private val onRemoteConnectListener: OnRemoteConnectionListener?
) : SimpleChannelInboundHandler<ProtoMessage>() {

    private val logger by loggerDelegate()

    @Throws(Exception::class)
    override fun channelInactive(ctx: ChannelHandlerContext) {
        logger.trace("channelInactive: {}", ctx)
        val tunnelId = ctx.channel().attr(AK_TUNNEL_ID).get()
        val sessionId = ctx.channel().attr(AK_SESSION_ID).get()
        if (tunnelId != null && sessionId != null) {
            localTcpClient.removeLocalChannel(tunnelId, sessionId)?.close()
        }
        val conn = ctx.channel().attr(AK_TUNNEL_CONNECTION).get()
        val extra = ctx.channel().attr(AK_CHANNEL_INACTIVE_EXTRA).get()
        onChannelStateListener.onChannelInactive(ctx, conn, extra)
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
        when (msg.type) {
            ProtoMessageType.PING -> doHandlePingMessage(ctx, msg as PingMessage)
            ProtoMessageType.RESPONSE_OK -> doHandleResponseOkMessage(ctx, msg as ResponseOkMessage)
            ProtoMessageType.RESPONSE_ERR -> doHandleResponseErrMessage(ctx, msg as ResponseErrMessage)
            ProtoMessageType.TRANSFER -> doHandleTransferMessage(ctx, msg as TransferMessage)
            ProtoMessageType.REMOTE_CONNECTED -> doHandleRemoteConnectedMessage(ctx, msg as RemoteConnectedMessage)
            ProtoMessageType.REMOTE_DISCONNECT -> doHandleRemoteDisconnectMessage(ctx, msg as RemoteDisconnectMessage)
            ProtoMessageType.FORCE_OFF -> doHandleForceOffMessage(ctx, msg as ForceOffMessage)
            else -> {
                // Nothing
            }
        }
    }

    /** Ping */
    @Throws(Exception::class)
    private fun doHandlePingMessage(ctx: ChannelHandlerContext, msg: PingMessage) {
        logger.trace("doHandlePingMessage : {}, {}", ctx, msg)
        ctx.writeAndFlush(ProtoMessage.PONG())
    }

    /** 隧道建立成功 */
    @Throws(Exception::class)
    private fun doHandleResponseOkMessage(ctx: ChannelHandlerContext, msg: ResponseOkMessage) {
        logger.trace("doHandleResponseOkMessage : {}, {}", ctx, msg)
        val tunnelRequest = TunnelRequest.fromBytes(msg.data)
        ctx.channel().attr(AK_TUNNEL_ID).set(msg.tunnelId)
        ctx.channel().attr(AK_TUNNEL_REQUEST).set(tunnelRequest)
        ctx.channel().attr(AK_CHANNEL_INACTIVE_EXTRA).set(null)
        val conn = ctx.channel().attr(AK_TUNNEL_CONNECTION).get()
        conn?.finalTunnelRequest = tunnelRequest
        logger.debug("Opened Tunnel: {}", tunnelRequest)
        onChannelStateListener.onChannelConnected(ctx, conn)
    }

    /** 隧道建立失败 */
    @Throws(Exception::class)
    private fun doHandleResponseErrMessage(ctx: ChannelHandlerContext, msg: ResponseErrMessage) {
        logger.trace("doHandleResponseErrMessage : {}, {}", ctx, msg)
        val errMsg = String(msg.data, StandardCharsets.UTF_8)
        ctx.channel().attr(AK_TUNNEL_ID).set(null)
        ctx.channel().attr(AK_TUNNEL_REQUEST).set(null)
        ctx.channel().attr(AK_CHANNEL_INACTIVE_EXTRA).set(ChannelInactiveExtra(false, Exception(errMsg)))
        ctx.channel().close()
        logger.debug("Open Tunnel Error: {}", errMsg)
    }

    /** 数据透传消息 */
    @Throws(Exception::class)
    private fun doHandleTransferMessage(ctx: ChannelHandlerContext, msg: TransferMessage) {
        logger.trace("doHandleTransferMessage : {}, {}", ctx, msg)
        ctx.channel().attr(AK_TUNNEL_ID).set(msg.tunnelId)
        ctx.channel().attr(AK_SESSION_ID).set(msg.sessionId)
        val tunnelRequest = ctx.channel().attr(AK_TUNNEL_REQUEST).get()
        when (tunnelRequest?.tunnelType) {
            TunnelType.TCP, TunnelType.HTTP, TunnelType.HTTPS -> {
                localTcpClient.acquireLocalChannel(
                    tunnelRequest.localAddr, tunnelRequest.localPort,
                    msg.tunnelId, msg.sessionId,
                    ctx.channel(),
                    object : LocalTcpClient.OnArriveLocalChannelCallback {
                        override fun onArrived(localChannel: Channel) {
                            logger.trace("onArrived: {}", localChannel)
                            localChannel.writeAndFlush(Unpooled.wrappedBuffer(msg.data))
                        }

                        override fun onUnableArrive(cause: Throwable) {
                            super.onUnableArrive(cause)
                            ctx.writeAndFlush(ProtoMessage.LOCAL_DISCONNECT(msg.tunnelId, msg.sessionId))
                        }
                    })
            }
            else -> {
                // Nothing
            }
        }
    }

    /** 连接本地隧道消息 */
    @Throws(Exception::class)
    private fun doHandleRemoteConnectedMessage(ctx: ChannelHandlerContext, msg: RemoteConnectedMessage) {
        logger.trace("doHandleRemoteConnectedMessage : {}, {}", ctx, msg)
        ctx.channel().attr(AK_TUNNEL_ID).set(msg.tunnelId)
        ctx.channel().attr(AK_SESSION_ID).set(msg.sessionId)
        val conn = try {
            RemoteConnection.fromBytes(msg.data)
        } catch (e: Exception) {
            null
        }
        if (conn != null) {
            onRemoteConnectListener?.onRemoteConnected(conn)
        }
        val tunnelRequest = ctx.channel().attr(AK_TUNNEL_REQUEST).get()
        if (tunnelRequest != null) {
            localTcpClient.acquireLocalChannel(
                tunnelRequest.localAddr, tunnelRequest.localPort,
                msg.tunnelId, msg.sessionId,
                ctx.channel()
            )
        }
    }

    /** 用户隧道断开消息 */
    @Throws(Exception::class)
    private fun doHandleRemoteDisconnectMessage(ctx: ChannelHandlerContext, msg: RemoteDisconnectMessage) {
        logger.trace("doHandleRemoteDisconnectMessage : {}, {}", ctx, msg)
        val conn = try {
            RemoteConnection.fromBytes(msg.data)
        } catch (e: Exception) {
            null
        }
        if (conn != null) {
            onRemoteConnectListener?.onRemoteDisconnect(conn)
        }
        localTcpClient.removeLocalChannel(msg.tunnelId, msg.sessionId)
            ?.writeAndFlush(Unpooled.EMPTY_BUFFER)
            ?.addListener(ChannelFutureListener.CLOSE)
    }

    /** 强制下线消息 */
    @Throws(Exception::class)
    private fun doHandleForceOffMessage(ctx: ChannelHandlerContext, msg: ForceOffMessage) {
        logger.trace("doHandleForceOffMessage : {}, {}", ctx, msg)
        ctx.channel().attr(AK_TUNNEL_ID).set(null)
        ctx.channel().attr(AK_TUNNEL_REQUEST).set(null)
        ctx.channel().attr(AK_CHANNEL_INACTIVE_EXTRA).set(ChannelInactiveExtra(true, Exception("ForceOff")))
        ctx.channel().writeAndFlush(ProtoMessage.FORCE_OFF_REPLY()).addListener(ChannelFutureListener.CLOSE)
    }

    interface OnChannelStateListener {
        fun onChannelInactive(
            ctx: ChannelHandlerContext,
            conn: TunnelConnectionDefaultImpl?,
            extra: ChannelInactiveExtra?
        ) {
        }

        fun onChannelConnected(ctx: ChannelHandlerContext, conn: TunnelConnectionDefaultImpl?) {}
    }

    class ChannelInactiveExtra(val forceOff: Boolean, val cause: Throwable?)

}