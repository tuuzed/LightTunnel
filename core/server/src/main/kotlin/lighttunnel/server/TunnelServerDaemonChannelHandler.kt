package lighttunnel.server

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import lighttunnel.base.entity.TunnelRequest
import lighttunnel.base.entity.TunnelType
import lighttunnel.base.proto.*
import lighttunnel.base.utils.IncIds
import lighttunnel.base.utils.loggerDelegate
import lighttunnel.server.http.HttpFd
import lighttunnel.server.http.HttpTunnel
import lighttunnel.server.tcp.TcpFd
import lighttunnel.server.tcp.TcpTunnel
import lighttunnel.server.utils.AK_SESSION_CHANNELS
import lighttunnel.server.utils.SessionChannels

internal class TunnelServerDaemonChannelHandler(
    private val tunnelRequestInterceptor: TunnelRequestInterceptor?,
    private val tunnelIds: IncIds,
    private val tcpTunnel: TcpTunnel?,
    private val httpTunnel: HttpTunnel?,
    private val httpsTunnel: HttpTunnel?,
    private val callback: Callback
) : SimpleChannelInboundHandler<ProtoMsg>() {

    private val logger by loggerDelegate()

    @Throws(Exception::class)
    override fun channelInactive(ctx: ChannelHandlerContext) {
        logger.trace("channelInactive: {}", ctx)
        ctx.channel().attr(AK_SESSION_CHANNELS).get()?.also { sc ->
            when (sc.tunnelRequest.tunnelType) {
                TunnelType.TCP -> callback.onChannelInactive(ctx, tcpTunnel?.stopTunnel(sc.tunnelRequest.remotePort))
                TunnelType.HTTP -> callback.onChannelInactive(ctx, httpTunnel?.stopTunnel(sc.tunnelRequest.host))
                TunnelType.HTTPS -> callback.onChannelInactive(ctx, httpsTunnel?.stopTunnel(sc.tunnelRequest.host))
                else -> {
                    // Nothing
                }
            }
        }
        ctx.channel().attr(AK_SESSION_CHANNELS).set(null)
        super.channelInactive(ctx)
    }

    @Throws(Exception::class)
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable?) {
        logger.trace("exceptionCaught: {}", ctx, cause)
        ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
    }

    @Throws(Exception::class)
    override fun channelRead0(ctx: ChannelHandlerContext, msg: ProtoMsg) {
        logger.trace("channelRead0: {}, {}", ctx, msg)
        when (msg) {
            ProtoMsgPing -> ctx.writeAndFlush(ProtoMsgPong)
            ProtoMsgForceOff -> {
                ctx.channel()?.writeAndFlush(Unpooled.EMPTY_BUFFER)?.addListener(ChannelFutureListener.CLOSE)
            }
            is ProtoMsgRequest -> {
                try {
                    val originalTunnelRequest = TunnelRequest.fromJson(msg.payload)
                    val finalTunnelRequest = tunnelRequestInterceptor?.intercept(originalTunnelRequest)
                        ?: originalTunnelRequest
                    logger.trace("TunnelRequest=> original: {}, final: {}", originalTunnelRequest, finalTunnelRequest)
                    when (finalTunnelRequest.tunnelType) {
                        TunnelType.TCP -> {
                            val tcpTunnel = tcpTunnel ?: throw ProtoException("TCP协议隧道未开启")
                            tcpTunnel.handleRequestMessage(ctx, finalTunnelRequest)
                        }
                        TunnelType.HTTP -> {
                            val httpTunnel = httpTunnel ?: throw ProtoException("HTTP协议隧道未开启")
                            httpTunnel.handleRequestMessage(ctx, finalTunnelRequest)
                        }
                        TunnelType.HTTPS -> {
                            val httpsTunnel = httpsTunnel ?: throw ProtoException("HTTPS协议隧道未开启")
                            httpsTunnel.handleRequestMessage(ctx, finalTunnelRequest)
                        }
                        else -> throw ProtoException("不支持的隧道类型")
                    }
                } catch (e: Exception) {
                    ctx.channel().writeAndFlush(
                        ProtoMsgResponse(false, 0, e.toString())
                    ).addListener(ChannelFutureListener.CLOSE)
                }
            }
            is ProtoMsgTransfer -> {
                val sessionChannels = ctx.channel().attr(AK_SESSION_CHANNELS).get() ?: return
                val sessionChannel = sessionChannels.getChannel(msg.sessionId)
                sessionChannel?.writeAndFlush(Unpooled.wrappedBuffer(msg.data))
            }
            is ProtoMsgRemoteDisconnect -> {
                val sessionChannels = ctx.channel().attr(AK_SESSION_CHANNELS).get() ?: return
                val sessionChannel = sessionChannels.removeChannel(msg.sessionId)
                // 解决 HTTP/1.x 数据传输问题
                sessionChannel?.writeAndFlush(Unpooled.EMPTY_BUFFER)?.addListener(ChannelFutureListener.CLOSE)
            }
            else -> {}
        }
    }

    @Throws(Exception::class)
    private fun TcpTunnel.handleRequestMessage(ctx: ChannelHandlerContext, tunnelRequest: TunnelRequest) {
        requireNotRegistered(tunnelRequest.remotePort)
        val tunnelId = tunnelIds.nextId
        val sessionChannels = SessionChannels(tunnelId, tunnelRequest, ctx.channel())
        ctx.channel().attr(AK_SESSION_CHANNELS).set(sessionChannels)
        val fd = startTunnel(null, tunnelRequest.remotePort, sessionChannels)
        callback.onChannelConnected(ctx, fd)
        ctx.channel().writeAndFlush(ProtoMsgResponse(true, tunnelId, tunnelRequest.toJsonString()))
    }

    @Throws(Exception::class)
    private fun HttpTunnel.handleRequestMessage(ctx: ChannelHandlerContext, tunnelRequest: TunnelRequest) {
        requireNotRegistered(tunnelRequest.host)
        val tunnelId = tunnelIds.nextId
        val sessionChannels = SessionChannels(tunnelId, tunnelRequest, ctx.channel())
        ctx.channel().attr(AK_SESSION_CHANNELS).set(sessionChannels)
        val fd = startTunnel(tunnelRequest.host, sessionChannels)
        callback.onChannelConnected(ctx, fd)
        ctx.channel().writeAndFlush(ProtoMsgResponse(true, tunnelId, tunnelRequest.toJsonString()))
    }

    internal interface Callback {
        fun onChannelInactive(ctx: ChannelHandlerContext, tcpFd: TcpFd?)
        fun onChannelInactive(ctx: ChannelHandlerContext, httpFd: HttpFd?)
        fun onChannelConnected(ctx: ChannelHandlerContext, tcpFd: TcpFd?)
        fun onChannelConnected(ctx: ChannelHandlerContext, httpFd: HttpFd?)
    }

}
