package lighttunnel.server

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import lighttunnel.common.entity.TunnelRequest
import lighttunnel.common.entity.TunnelType
import lighttunnel.common.exception.LightTunnelException
import lighttunnel.common.proto.msg.*
import lighttunnel.common.utils.*
import lighttunnel.server.http.HttpDescriptor
import lighttunnel.server.http.HttpTunnel
import lighttunnel.server.tcp.TcpDescriptor
import lighttunnel.server.tcp.TcpTunnel
import lighttunnel.server.utils.AK_AES128_KEY
import lighttunnel.server.utils.AK_SESSION_CHANNELS
import lighttunnel.server.utils.AK_TUNNEL_DESCRIPTOR
import lighttunnel.server.utils.AK_WATCHDOG_TIME_MILLIS

@Suppress("DuplicatedCode")
internal class ServerTunnelDaemonChannelHandler(
    private val tunnelRequestInterceptor: TunnelRequestInterceptor?,
    private val tunnelIds: IncIds,
    private val tcpTunnel: TcpTunnel?,
    private val httpTunnel: HttpTunnel?,
    private val httpsTunnel: HttpTunnel?,
    private val callback: Callback?
) : SimpleChannelInboundHandler<ProtoMsg>() {

    private val logger by injectLogger()

    @Throws(Exception::class)
    override fun channelInactive(ctx: ChannelHandlerContext) {
        logger.trace("channelInactive: {}", ctx)
        ctx.channel().attr(AK_SESSION_CHANNELS).get()?.also { sc ->
            when (sc.tunnelRequest.tunnelType) {
                TunnelType.TCP -> {
                    val descriptor = tcpTunnel?.stopTunnel(sc.tunnelRequest.remotePort)
                    callback?.onChannelInactive(ctx, descriptor)
                }
                TunnelType.HTTP -> {
                    val descriptor = httpTunnel?.stopTunnel(sc.tunnelRequest.vhost)
                    callback?.onChannelInactive(ctx, descriptor)
                }
                TunnelType.HTTPS -> {
                    val descriptor = httpsTunnel?.stopTunnel(sc.tunnelRequest.vhost)
                    callback?.onChannelInactive(ctx, descriptor)
                }
                else -> {
                    // Nothing
                }
            }
        }
        ctx.channel().attr(AK_AES128_KEY).set(null)
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
        ctx.channel().attr(AK_WATCHDOG_TIME_MILLIS).set(System.currentTimeMillis())
        when (msg) {
            ProtoMsgPing -> ctx.writeAndFlush(ProtoMsgPong)
            ProtoMsgForceOff -> {
                ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
            }
            is ProtoMsgHandshake -> {
                if (msg.rawBytes.isNotEmpty()) {
                    val rsaPubKey = msg.rawBytes
                    val aes128Key = CryptoUtils.randomAES128Key()
                    ctx.channel().attr(AK_AES128_KEY).set(aes128Key)
                    val compressedAndData = CryptoUtils.encryptRSA(aes128Key, rsaPubKey).tryGZip()
                    ctx.channel().writeAndFlush(ProtoMsgHandshake(compressedAndData.second, compressedAndData.first))
                } else {
                    ctx.channel().writeAndFlush(ProtoMsgHandshake(emptyBytes, false))
                }
            }
            is ProtoMsgRequest -> {
                try {
                    val tunnelRequest = TunnelRequest.internalFromJson(msg.payload).let {
                        logger.trace("TunnelRequest=> original: {}", it)
                        tunnelRequestInterceptor?.intercept(it) ?: it
                    }
                    logger.trace("TunnelRequest=> final: {}", tunnelRequest)
                    when (tunnelRequest.tunnelType) {
                        TunnelType.TCP -> {
                            val tcpTunnel = tcpTunnel ?: throw LightTunnelException("TCP协议隧道未开启")
                            tcpTunnel.handleRequestMessage(ctx, tunnelRequest)
                        }
                        TunnelType.HTTP -> {
                            val httpTunnel = httpTunnel ?: throw LightTunnelException("HTTP协议隧道未开启")
                            httpTunnel.handleRequestMessage(ctx, tunnelRequest)
                        }
                        TunnelType.HTTPS -> {
                            val httpsTunnel = httpsTunnel ?: throw LightTunnelException("HTTPS协议隧道未开启")
                            httpsTunnel.handleRequestMessage(ctx, tunnelRequest)
                        }
                        else -> throw LightTunnelException("不支持的隧道类型")
                    }
                } catch (e: Exception) {
                    logger.error("ProtoMsgRequest: ", e)
                    val aes128Key = ctx.channel().attr(AK_AES128_KEY).get()
                    val compressedAndData = e.toString().toByteArray()
                        .tryGZip()
                        .let {
                            it.first to if (it.second.isNotEmpty() && aes128Key != null) it.second.tryEncryptAES128(
                                aes128Key
                            ) else it.second
                        }
                    ctx.channel().writeAndFlush(
                        ProtoMsgResponse(
                            false,
                            0,
                            compressedAndData.second,
                            aes128Key,
                            compressedAndData.first,
                        )
                    ).addListener(ChannelFutureListener.CLOSE)
                }
            }
            is ProtoMsgTransfer -> {
                val sessionChannels = ctx.channel().attr(AK_SESSION_CHANNELS).get() ?: return
                val sessionChannel = sessionChannels.getSessionChannel(msg.sessionId)
                sessionChannel?.writeAndFlush(Unpooled.wrappedBuffer(msg.rawBytes))
            }
            is ProtoMsgRemoteDisconnect -> {
                val sessionChannels = ctx.channel().attr(AK_SESSION_CHANNELS).get() ?: return
                val sessionChannel = sessionChannels.removeSessionChannel(msg.sessionId)
                // 解决 HTTP/1.x 数据传输问题
                sessionChannel?.writeAndFlush(Unpooled.EMPTY_BUFFER)?.addListener(ChannelFutureListener.CLOSE)
            }
            else -> {}
        }
    }

    @Throws(Exception::class)
    private fun TcpTunnel.handleRequestMessage(ctx: ChannelHandlerContext, tunnelRequest: TunnelRequest) {
        requireUnregistered(tunnelRequest.remotePort)
        val tunnelId = tunnelIds.nextId
        val sessionChannels = SessionChannels(tunnelId, tunnelRequest, ctx.channel())
        ctx.channel().attr(AK_SESSION_CHANNELS).set(sessionChannels)
        val descriptor = startTunnel(null, tunnelRequest.remotePort, sessionChannels)
        ctx.channel().attr(AK_TUNNEL_DESCRIPTOR).set(descriptor)
        callback?.onChannelConnected(ctx, descriptor)
        val aes128Key = ctx.channel().attr(AK_AES128_KEY).get()
        val compressedAndData = tunnelRequest.asJsonString().toByteArray()
            .tryGZip()
            .let {
                it.first to if (it.second.isNotEmpty() && aes128Key != null) it.second.tryEncryptAES128(aes128Key) else it.second
            }
        ctx.channel().writeAndFlush(
            ProtoMsgResponse(
                true,
                tunnelId,
                compressedAndData.second,
                aes128Key,
                compressedAndData.first,
            )
        )
    }

    @Throws(Exception::class)
    private fun HttpTunnel.handleRequestMessage(ctx: ChannelHandlerContext, tunnelRequest: TunnelRequest) {
        requireUnregistered(tunnelRequest.vhost)
        val tunnelId = tunnelIds.nextId
        val sessionChannels = SessionChannels(tunnelId, tunnelRequest, ctx.channel())
        ctx.channel().attr(AK_SESSION_CHANNELS).set(sessionChannels)
        val descriptor = startTunnel(tunnelRequest.vhost, sessionChannels)
        ctx.channel().attr(AK_TUNNEL_DESCRIPTOR).set(descriptor)
        callback?.onChannelConnected(ctx, descriptor)
        val aes128Key = ctx.channel().attr(AK_AES128_KEY).get()
        val compressedAndData = tunnelRequest.asJsonString().toByteArray()
            .tryGZip()
            .let {
                it.first to if (it.second.isNotEmpty() && aes128Key != null) it.second.tryEncryptAES128(aes128Key) else it.second
            }
        ctx.channel().writeAndFlush(
            ProtoMsgResponse(
                true,
                tunnelId,
                compressedAndData.second,
                aes128Key,
                compressedAndData.first,
            )
        )
    }

    internal interface Callback {
        fun onChannelConnected(ctx: ChannelHandlerContext, descriptor: TcpDescriptor?) {}
        fun onChannelInactive(ctx: ChannelHandlerContext, descriptor: TcpDescriptor?) {}
        fun onChannelInactive(ctx: ChannelHandlerContext, descriptor: HttpDescriptor?) {}
        fun onChannelConnected(ctx: ChannelHandlerContext, descriptor: HttpDescriptor?) {}
    }

}
