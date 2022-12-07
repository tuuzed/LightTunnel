package lighttunnel.client

import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import lighttunnel.client.conn.DefaultTunnelConn
import lighttunnel.client.extra.ChannelInactiveExtra
import lighttunnel.client.local.LocalTcpClient
import lighttunnel.client.utils.*
import lighttunnel.common.entity.RemoteConnection
import lighttunnel.common.entity.TunnelRequest
import lighttunnel.common.entity.TunnelType
import lighttunnel.common.exception.LightTunnelException
import lighttunnel.common.proto.msg.*
import lighttunnel.common.utils.CryptoUtils
import lighttunnel.common.utils.injectLogger
import lighttunnel.common.utils.tryEncryptAES128
import lighttunnel.common.utils.tryGZip

internal class ClientTunnelDaemonChannelHandler(
    private val localTcpClient: LocalTcpClient,
    private val callback: Callback,
    private val clientListener: ClientListener?,
) : SimpleChannelInboundHandler<ProtoMsg>() {

    private val logger by injectLogger()

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
        callback.onChannelInactive(ctx, conn, extra)
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
                ctx.channel().attr(AK_CHANNEL_INACTIVE_EXTRA).set(
                    ChannelInactiveExtra(true, LightTunnelException("ForceOff"))
                )
                ctx.channel().writeAndFlush(ProtoMsgForceOffReply).addListener(ChannelFutureListener.CLOSE)
            }

            is ProtoMsgHandshake -> {
                // 发送连接请求
                val tunnelRequest = ctx.channel().attr(AK_TUNNEL_REQUEST).get()
                val rsaPriKey = ctx.channel().attr(AK_RSA_PRI_KEY).get()
                ctx.channel().writeAndFlush(
                    if (rsaPriKey != null) {
                        val aes128Key = CryptoUtils.decryptRSA(msg.rawBytes, rsaPriKey)
                        ctx.channel().attr(AK_AES128_KEY).set(aes128Key)
                        val compressedAndData = tunnelRequest.asJsonString()
                            .toByteArray()
                            .tryGZip()
                            .let { it.first to it.second.tryEncryptAES128(aes128Key) }
                        ProtoMsgRequest(compressedAndData.second, aes128Key, compressedAndData.first)
                    } else {
                        val compressedAndData = tunnelRequest.asJsonString()
                            .toByteArray()
                            .tryGZip()
                        ProtoMsgRequest(compressedAndData.second, null, compressedAndData.first)
                    }
                )
            }

            is ProtoMsgResponse -> {
                if (msg.status) {
                    val tunnelRequest = TunnelRequest.internalFromJson(msg.payload)
                    ctx.channel().attr(AK_TUNNEL_ID).set(msg.tunnelId)
                    ctx.channel().attr(AK_TUNNEL_REQUEST).set(tunnelRequest)
                    ctx.channel().attr(AK_CHANNEL_INACTIVE_EXTRA).set(null)
                    val conn = ctx.channel().attr(AK_TUNNEL_CONN).get()
                    conn?.finalTunnelRequest = tunnelRequest
                    logger.debug("Opened Tunnel: {}", tunnelRequest)
                    callback.onChannelConnected(ctx, conn)
                } else {
                    ctx.channel().attr(AK_TUNNEL_ID).set(null)
                    ctx.channel().attr(AK_TUNNEL_REQUEST).set(null)
                    ctx.channel().attr(AK_CHANNEL_INACTIVE_EXTRA).set(
                        ChannelInactiveExtra(false, LightTunnelException(msg.payload))
                    )
                    ctx.channel().close()
                    logger.debug("Open Tunnel Error: {}", msg.payload)
                }
            }

            is ProtoMsgTransfer -> {
                ctx.channel().attr(AK_TUNNEL_ID).set(msg.tunnelId)
                ctx.channel().attr(AK_SESSION_ID).set(msg.sessionId)
                val tunnelRequest = ctx.channel().attr(AK_TUNNEL_REQUEST).get()
                when (tunnelRequest?.tunnelType) {
                    TunnelType.TCP, TunnelType.HTTP, TunnelType.HTTPS -> {
                        localTcpClient.acquireLocalChannel(tunnelRequest.localIp,
                            tunnelRequest.localPort,
                            msg.tunnelId,
                            msg.sessionId,
                            ctx.channel(),
                            object : LocalTcpClient.OnArriveLocalChannelCallback {
                                override fun onArrived(localChannel: Channel) {
                                    logger.trace("onArrived: {}", localChannel)
                                    localChannel.writeAndFlush(Unpooled.wrappedBuffer(msg.rawBytes))
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
                    clientListener?.onRemoteConnected(conn)
                }
                val tunnelRequest = ctx.channel().attr(AK_TUNNEL_REQUEST).get()
                if (tunnelRequest != null) {
                    localTcpClient.acquireLocalChannel(
                        tunnelRequest.localIp, tunnelRequest.localPort, msg.tunnelId, msg.sessionId, ctx.channel()
                    )
                }
            }

            is ProtoMsgRemoteDisconnect -> {
                val conn = runCatching { RemoteConnection.fromJson(msg.payload) }.getOrNull()
                if (conn != null) {
                    clientListener?.onRemoteDisconnect(conn)
                }
                localTcpClient.removeLocalChannel(msg.tunnelId, msg.sessionId)?.writeAndFlush(Unpooled.EMPTY_BUFFER)
                    ?.addListener(ChannelFutureListener.CLOSE)
            }

            else -> {}
        }
    }

    interface Callback {
        fun onChannelInactive(ctx: ChannelHandlerContext, conn: DefaultTunnelConn?, extra: ChannelInactiveExtra?) {}
        fun onChannelConnected(ctx: ChannelHandlerContext, conn: DefaultTunnelConn?) {}
    }

}
