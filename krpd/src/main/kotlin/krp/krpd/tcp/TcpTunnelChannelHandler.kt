package krp.krpd.tcp

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import krp.common.entity.RemoteConnection
import krp.common.proto.msg.ProtoMsgRemoteConnected
import krp.common.proto.msg.ProtoMsgRemoteDisconnect
import krp.common.proto.msg.ProtoMsgTransfer
import krp.common.utils.emptyBytes
import krp.common.utils.injectLogger
import krp.common.utils.tryEncryptAES128
import krp.common.utils.tryGZip
import krp.krpd.utils.AK_AES128_KEY
import krp.krpd.utils.AK_SESSION_ID
import java.net.InetSocketAddress

@Suppress("DuplicatedCode")
internal class TcpTunnelChannelHandler(
    private val registry: TcpRegistry
) : SimpleChannelInboundHandler<ByteBuf>() {
    private val logger by injectLogger()

    override fun channelActive(ctx: ChannelHandlerContext) {
        logger.trace("channelActive: {}", ctx)
        val fd = ctx.fd
        if (fd != null) {
            var sessionId = ctx.channel().attr(AK_SESSION_ID).get()
            if (sessionId == null) {
                sessionId = fd.putSessionChannel(ctx.channel())
                ctx.channel().attr(AK_SESSION_ID).set(sessionId)
            }
            val aes128Key = fd.tunnelChannel.attr(AK_AES128_KEY).get()
            val compressedAndData =
                (RemoteConnection(ctx.channel().remoteAddress()).asJsonString()?.toByteArray() ?: emptyBytes)
                    .tryGZip()
                    .let {
                        it.first to if (it.second.isNotEmpty() && aes128Key != null) it.second.tryEncryptAES128(aes128Key) else it.second
                    }
            fd.tunnelChannel.writeAndFlush(
                ProtoMsgRemoteConnected(
                    fd.tunnelId,
                    sessionId,
                    compressedAndData.second,
                    aes128Key,
                    compressedAndData.first,
                )
            )
        } else {
            ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
        }
        super.channelActive(ctx)
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        logger.trace("channelInactive: {}", ctx)
        val fd = ctx.fd
        if (fd != null) {
            val sessionId = ctx.channel().attr(AK_SESSION_ID).get()
            if (sessionId != null) {
                val sessionChannel = fd.removeSessionChannel(sessionId)
                // 解决 HTTP/1.x 数据传输问题
                sessionChannel?.writeAndFlush(Unpooled.EMPTY_BUFFER)?.addListener(ChannelFutureListener.CLOSE)
            }

            ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener {
                val aes128Key = fd.tunnelChannel.attr(AK_AES128_KEY).get()
                val compressedAndData =
                    (RemoteConnection(ctx.channel().remoteAddress()).asJsonString()?.toByteArray() ?: emptyBytes)
                        .tryGZip()
                        .let {
                            it.first to if (it.second.isNotEmpty() && aes128Key != null) it.second.tryEncryptAES128(aes128Key) else it.second
                        }
                fd.tunnelChannel.writeAndFlush(
                    ProtoMsgRemoteDisconnect(
                        fd.tunnelId,
                        sessionId ?: 0,
                        compressedAndData.second,
                        aes128Key,
                        compressedAndData.first,
                    )
                )
            }
        }
        ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
        super.channelInactive(ctx)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
        logger.trace("exceptionCaught: {}, {}", ctx, cause)
        ctx?.channel()?.writeAndFlush(Unpooled.EMPTY_BUFFER)?.addListener(ChannelFutureListener.CLOSE)
    }

    override fun channelRead0(ctx: ChannelHandlerContext?, msg: ByteBuf?) {
        ctx ?: return
        msg ?: return
        logger.trace("channelRead0: {}, {}", ctx, msg)
        val sessionId = ctx.channel().attr(AK_SESSION_ID).get() ?: return
        val fd = ctx.fd ?: return
        val aes128Key = fd.tunnelChannel.attr(AK_AES128_KEY).get()
        val compressedAndData = (ByteBufUtil.getBytes(msg) ?: emptyBytes)
            .tryGZip()
            .let {
                it.first to if (it.second.isNotEmpty() && aes128Key != null) it.second.tryEncryptAES128(aes128Key) else it.second
            }
        fd.tunnelChannel.writeAndFlush(
            ProtoMsgTransfer(
                fd.tunnelId,
                sessionId,
                compressedAndData.second,
                aes128Key,
                compressedAndData.first,
            )
        )
    }

    private val ChannelHandlerContext?.fd: DefaultTcpFd?
        get() {
            this ?: return null
            val sa = this.channel().localAddress()
            return if (sa is InetSocketAddress) registry.getTcpFd(sa.port) else null
        }

}
