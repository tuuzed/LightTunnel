package lighttunnel.server.tcp

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import lighttunnel.common.entity.RemoteConn
import lighttunnel.common.proto.msg.ProtoMsgRemoteConnected
import lighttunnel.common.proto.msg.ProtoMsgRemoteDisconnect
import lighttunnel.common.proto.msg.ProtoMsgTransfer
import lighttunnel.common.utils.emptyBytes
import lighttunnel.common.utils.injectLogger
import lighttunnel.common.utils.tryEncryptAES128
import lighttunnel.common.utils.tryGZip
import lighttunnel.server.utils.AK_AES128_KEY
import lighttunnel.server.utils.AK_SESSION_ID
import java.net.InetSocketAddress

@Suppress("DuplicatedCode")
internal class TcpTunnelChannelHandler(
    private val registry: TcpRegistry
) : SimpleChannelInboundHandler<ByteBuf>() {
    private val logger by injectLogger()

    override fun channelActive(ctx: ChannelHandlerContext) {
        logger.trace("channelActive: {}", ctx)
        val descriptor = ctx.descriptor
        if (descriptor != null) {
            var sessionId = ctx.channel().attr(AK_SESSION_ID).get()
            if (sessionId == null) {
                sessionId = descriptor.putSessionChannel(ctx.channel())
                ctx.channel().attr(AK_SESSION_ID).set(sessionId)
            }
            val aes128Key = descriptor.tunnelChannel.attr(AK_AES128_KEY).get()
            val compressedAndData =
                (RemoteConn(ctx.channel().remoteAddress()).asJsonString()?.toByteArray() ?: emptyBytes)
                    .tryGZip()
                    .let {
                        it.first to if (it.second.isNotEmpty() && aes128Key != null) it.second.tryEncryptAES128(aes128Key) else it.second
                    }
            descriptor.tunnelChannel.writeAndFlush(
                ProtoMsgRemoteConnected(
                    descriptor.tunnelId,
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
        val descriptor = ctx.descriptor
        if (descriptor != null) {
            val sessionId = ctx.channel().attr(AK_SESSION_ID).get()
            if (sessionId != null) {
                val sessionChannel = descriptor.removeSessionChannel(sessionId)
                // 解决 HTTP/1.x 数据传输问题
                sessionChannel?.writeAndFlush(Unpooled.EMPTY_BUFFER)?.addListener(ChannelFutureListener.CLOSE)
            }

            ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener {
                val aes128Key = descriptor.tunnelChannel.attr(AK_AES128_KEY).get()
                val compressedAndData =
                    (RemoteConn(ctx.channel().remoteAddress()).asJsonString()?.toByteArray() ?: emptyBytes)
                        .tryGZip()
                        .let {
                            it.first to if (it.second.isNotEmpty() && aes128Key != null) it.second.tryEncryptAES128(aes128Key) else it.second
                        }
                descriptor.tunnelChannel.writeAndFlush(
                    ProtoMsgRemoteDisconnect(
                        descriptor.tunnelId,
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
        val descriptor = ctx.descriptor ?: return
        val aes128Key = descriptor.tunnelChannel.attr(AK_AES128_KEY).get()
        val compressedAndData = (ByteBufUtil.getBytes(msg) ?: emptyBytes)
            .tryGZip()
            .let {
                it.first to if (it.second.isNotEmpty() && aes128Key != null) it.second.tryEncryptAES128(aes128Key) else it.second
            }
        descriptor.tunnelChannel.writeAndFlush(
            ProtoMsgTransfer(
                descriptor.tunnelId,
                sessionId,
                compressedAndData.second,
                aes128Key,
                compressedAndData.first,
            )
        )
    }

    private val ChannelHandlerContext?.descriptor: DefaultTcpDescriptor?
        get() {
            this ?: return null
            val sa = this.channel().localAddress()
            return if (sa is InetSocketAddress) registry.getTcpDescriptor(sa.port) else null
        }

}
