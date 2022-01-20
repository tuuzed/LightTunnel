package lighttunnel.server

import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import lighttunnel.base.RemoteConnection
import lighttunnel.base.TunnelRequest
import lighttunnel.base.TunnelType
import lighttunnel.base.proto.*
import lighttunnel.base.utils.loggerDelegate
import lighttunnel.server.conn.impl.TunnelConnImpl
import lighttunnel.server.extra.ChannelInactiveExtra
import lighttunnel.server.listener.OnRemoteConnectionListener
import lighttunnel.server.local.LocalTcpClient
import lighttunnel.server.utils.*

internal class TunnelClientDaemonChannelHandler(
    private val localTcpClient: LocalTcpClient,
    private val onChannelStateListener: OnChannelStateListener,
    private val onRemoteConnectListener: OnRemoteConnectionListener?
) : SimpleChannelInboundHandler<ProtoMsg>() {

    private val logger by loggerDelegate()

    @Throws(Exception::class)
    override fun channelInactive(ctx: ChannelHandlerContext) {
        logger.trace("channelInactive: {}", ctx)
        val tunnelId = ctx.channel().attr(AK_TUNNEL_ID).get()
        val sessionId = ctx.channel().attr(AK_SESSION_ID).get()
        if (tunnelId != null && sessionId != null) {
            localTcpClient.removeLocalChannel(tunnelId, sessionId)?.close()
        }
        val conn = ctx.channel().attr(AK_TUNNEL_CONN).get()
        val extra = ctx.channel().attr(AK_CHANNEL_INACTIVE_EXTRA).get()
        onChannelStateListener.onChannelInactive(ctx, conn, extra)
        super.channelInactive(ctx)
    }

    @Throws(Exception::class)
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable?) {
        logger.trace("exceptionCaught: {}", ctx, cause)
        ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
    }

    @Throws(Exception::class)
    override fun channelRead0(ctx: ChannelHandlerContext, msg: ProtoMsg) {
        logger.trace("channelRead0 : {}, {}", ctx, msg)
        when (msg) {
            ProtoMsgPing -> ctx.writeAndFlush(ProtoMsgPong)
            ProtoMsgForceOff -> {
                ctx.channel().attr(AK_TUNNEL_ID).set(null)
                ctx.channel().attr(AK_TUNNEL_REQUEST).set(null)
                ctx.channel().attr(AK_CHANNEL_INACTIVE_EXTRA).set(ChannelInactiveExtra(true, Exception("ForceOff")))
                ctx.channel().writeAndFlush(ProtoMsgForceOffReply).addListener(ChannelFutureListener.CLOSE)
            }
            is ProtoMsgResponse -> {
                if (msg.status) {
                    val tunnelRequest = TunnelRequest.fromJson(msg.payload)
                    ctx.channel().attr(AK_TUNNEL_ID).set(msg.tunnelId)
                    ctx.channel().attr(AK_TUNNEL_REQUEST).set(tunnelRequest)
                    ctx.channel().attr(AK_CHANNEL_INACTIVE_EXTRA).set(null)
                    val conn = ctx.channel().attr(AK_TUNNEL_CONN).get()
                    conn?.finalTunnelRequest = tunnelRequest
                    logger.debug("Opened Tunnel: {}", tunnelRequest)
                    onChannelStateListener.onChannelConnected(ctx, conn)
                } else {
                    val errMsg = msg.payload
                    ctx.channel().attr(AK_TUNNEL_ID).set(null)
                    ctx.channel().attr(AK_TUNNEL_REQUEST).set(null)
                    ctx.channel().attr(AK_CHANNEL_INACTIVE_EXTRA).set(ChannelInactiveExtra(false, Exception(errMsg)))
                    ctx.channel().close()
                    logger.debug("Open Tunnel Error: {}", errMsg)
                }
            }
            is ProtoMsgTransfer -> {
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
                                    ctx.writeAndFlush(ProtoMsgLocalDisconnect(msg.tunnelId, msg.sessionId))
                                }
                            })
                    }
                    else -> {
                        // Nothing
                    }
                }
            }
            is ProtoMsgRemoteConnected -> {
                ctx.channel().attr(AK_TUNNEL_ID).set(msg.tunnelId)
                ctx.channel().attr(AK_SESSION_ID).set(msg.sessionId)
                val conn = runCatching { RemoteConnection.fromJson(msg.payload) }.getOrNull()
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
            is ProtoMsgRemoteDisconnect -> {
                val conn = runCatching { RemoteConnection.fromJson(msg.payload) }.getOrNull()
                if (conn != null) {
                    onRemoteConnectListener?.onRemoteDisconnect(conn)
                }
                localTcpClient.removeLocalChannel(msg.tunnelId, msg.sessionId)
                    ?.writeAndFlush(Unpooled.EMPTY_BUFFER)
                    ?.addListener(ChannelFutureListener.CLOSE)
            }
            else -> {}
        }
    }

    interface OnChannelStateListener {
        fun onChannelInactive(ctx: ChannelHandlerContext, conn: TunnelConnImpl?, extra: ChannelInactiveExtra?) {}
        fun onChannelConnected(ctx: ChannelHandlerContext, conn: TunnelConnImpl?) {}
    }

}
