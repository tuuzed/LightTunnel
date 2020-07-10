@file:Suppress("UNUSED_PARAMETER")

package lighttunnel.client

import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import lighttunnel.client.conn.TunnelConnection
import lighttunnel.client.local.LocalTcpClient
import lighttunnel.client.util.*
import lighttunnel.logger.loggerDelegate
import lighttunnel.proto.ProtoMessage
import lighttunnel.proto.ProtoMessageType
import lighttunnel.proto.RemoteInfo
import lighttunnel.proto.TunnelRequest
import lighttunnel.util.LongUtil
import java.nio.charset.StandardCharsets

internal class TunnelClientChannelHandler(
    private val localTcpClient: LocalTcpClient,
    private val onChannelStateListener: OnChannelStateListener,
    private val onRemoteConnectListener: TunnelClient.OnRemoteConnectListener?
) : SimpleChannelInboundHandler<ProtoMessage>() {
    private val logger by loggerDelegate()

    @Throws(Exception::class)
    override fun channelInactive(ctx: ChannelHandlerContext?) {
        logger.trace("channelInactive: {}", ctx)
        if (ctx == null) {
            super.channelInactive(ctx)
            return
        }
        val tunnelId = ctx.channel().attr(AK_TUNNEL_ID).get()
        val sessionId = ctx.channel().attr(AK_SESSION_ID).get()
        if (tunnelId != null && sessionId != null) {
            localTcpClient.removeLocalChannel(tunnelId, sessionId)?.close()
        }
        val fd = ctx.channel().attr(AK_TUNNEL_CONNECTION).get()
        val cause = ctx.channel().attr(AK_ERROR_CAUSE).get()
        val forcedOffline = ctx.channel().attr(AK_FORCED_OFFLINE).get() ?: false
        onChannelStateListener.onChannelInactive(ctx, fd, forcedOffline, cause)
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
            ProtoMessageType.PING -> doHandlePingMessage(ctx, msg)
            ProtoMessageType.RESPONSE_OK -> doHandleResponseOkMessage(ctx, msg)
            ProtoMessageType.RESPONSE_ERR -> doHandleResponseErrMessage(ctx, msg)
            ProtoMessageType.TRANSFER -> doHandleTransferMessage(ctx, msg)
            ProtoMessageType.REMOTE_CONNECTED -> doHandleRemoteConnectedMessage(ctx, msg)
            ProtoMessageType.REMOTE_DISCONNECT -> doHandleRemoteDisconnectMessage(ctx, msg)
            ProtoMessageType.FORCED_OFFLINE -> doHandleForcedOfflineMessage(ctx, msg)
            else -> {
            }
        }
    }

    /** Ping */
    @Throws(Exception::class)
    private fun doHandlePingMessage(ctx: ChannelHandlerContext, msg: ProtoMessage) {
        logger.trace("doHandlePingMessage : {}, {}", ctx, msg)
        ctx.writeAndFlush(ProtoMessage(ProtoMessageType.PONG))
    }

    /** 隧道建立成功 */
    @Throws(Exception::class)
    private fun doHandleResponseOkMessage(ctx: ChannelHandlerContext, msg: ProtoMessage) {
        logger.trace("doHandleResponseOkMessage : {}, {}", ctx, msg)
        val request = TunnelRequest.fromBytes(msg.data)
        ctx.run {
            channel().attr(AK_TUNNEL_ID).set(msg.tunnelId)
            channel().attr(AK_TUNNEL_REQUEST).set(request)
            channel().attr(AK_ERROR_CAUSE).set(null)
        }
        val fd = ctx.channel().attr(AK_TUNNEL_CONNECTION).get()
        fd?.finalTunnelRequest = request
        logger.debug("Opened Tunnel: {}", request)
        onChannelStateListener.onChannelConnected(ctx, fd)
    }

    /** 隧道建立失败 */
    @Throws(Exception::class)
    private fun doHandleResponseErrMessage(ctx: ChannelHandlerContext, msg: ProtoMessage) {
        logger.trace("doHandleResponseErrMessage : {}, {}", ctx, msg)
        val errMessage = String(msg.head, StandardCharsets.UTF_8)
        ctx.channel().attr(AK_TUNNEL_ID).set(null)
        ctx.channel().attr(AK_TUNNEL_REQUEST).set(null)
        ctx.channel().attr(AK_ERROR_CAUSE).set(Exception(errMessage))
        ctx.channel().close()
        logger.debug("Open Tunnel Error: {}", errMessage)
    }

    /** 数据透传消息 */
    @Throws(Exception::class)
    private fun doHandleTransferMessage(ctx: ChannelHandlerContext, msg: ProtoMessage) {
        logger.trace("doHandleTransferMessage : {}, {}", ctx, msg)
        ctx.channel().attr(AK_TUNNEL_ID).set(msg.tunnelId)
        ctx.channel().attr(AK_SESSION_ID).set(msg.sessionId)
        val tunnelRequest = ctx.channel().attr(AK_TUNNEL_REQUEST).get()
        when (tunnelRequest?.type) {
            TunnelRequest.Type.TCP, TunnelRequest.Type.HTTP, TunnelRequest.Type.HTTPS -> {
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
                            val head = LongUtil.toBytes(msg.tunnelId, msg.sessionId)
                            ctx.writeAndFlush(ProtoMessage(ProtoMessageType.LOCAL_DISCONNECT, head))
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
    private fun doHandleRemoteConnectedMessage(ctx: ChannelHandlerContext, msg: ProtoMessage) {
        logger.trace("doHandleRemoteConnectedMessage : {}, {}", ctx, msg)
        ctx.channel().attr(AK_TUNNEL_ID).set(msg.tunnelId)
        ctx.channel().attr(AK_SESSION_ID).set(msg.sessionId)
        val remoteInfo = try {
            RemoteInfo.fromBytes(msg.data)
        } catch (e: Exception) {
            null
        }
        onRemoteConnectListener?.onRemoteConnected(remoteInfo)
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
    private fun doHandleRemoteDisconnectMessage(ctx: ChannelHandlerContext, msg: ProtoMessage) {
        logger.trace("doHandleRemoteDisconnectMessage : {}, {}", ctx, msg)
        val remoteInfo = try {
            RemoteInfo.fromBytes(msg.data)
        } catch (e: Exception) {
            null
        }
        onRemoteConnectListener?.onRemoteDisconnect(remoteInfo)
        localTcpClient.removeLocalChannel(msg.tunnelId, msg.sessionId)
            ?.writeAndFlush(Unpooled.EMPTY_BUFFER)
            ?.addListener(ChannelFutureListener.CLOSE)
    }

    /** 强制下线消息 */
    @Throws(Exception::class)
    private fun doHandleForcedOfflineMessage(ctx: ChannelHandlerContext, msg: ProtoMessage) {
        logger.trace("doHandleForcedOfflineMessage : {}, {}", ctx, msg)
        ctx.channel().attr(AK_TUNNEL_ID).set(null)
        ctx.channel().attr(AK_TUNNEL_REQUEST).set(null)
        ctx.channel().attr(AK_FORCED_OFFLINE).set(true)
        ctx.channel().attr(AK_ERROR_CAUSE).set(Exception("ForcedOffline"))
        ctx.channel().writeAndFlush(ProtoMessage(ProtoMessageType.FORCED_OFFLINE_REPLY)).addListener(ChannelFutureListener.CLOSE)
    }

    internal interface OnChannelStateListener {
        fun onChannelInactive(ctx: ChannelHandlerContext, conn: TunnelConnection?, forceOffline: Boolean, cause: Throwable?) {}
        fun onChannelConnected(ctx: ChannelHandlerContext, conn: TunnelConnection?) {}
    }

}