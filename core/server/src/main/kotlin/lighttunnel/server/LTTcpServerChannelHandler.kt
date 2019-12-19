package lighttunnel.server

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import lighttunnel.logging.logger
import lighttunnel.proto.LTCommand
import lighttunnel.proto.LTMassage
import lighttunnel.util.long2Bytes
import lighttunnel.util.toBytes
import java.net.InetSocketAddress

class LTTcpServerChannelHandler(
    private val registry: LTTcpRegistry
) : SimpleChannelInboundHandler<ByteBuf>() {
    private val logger by logger()

    override fun channelActive(ctx: ChannelHandlerContext?) {
        if (ctx != null) {
            val sa = ctx.channel().localAddress() as InetSocketAddress
            val descriptor = registry.getDescriptorByPort(sa.port)
            if (descriptor != null) {
                var sessionId = ctx.channel().attr<Long>(AK_SESSION_ID).get()
                if (sessionId == null) {
                    sessionId = descriptor.sessionPool.putChannel(ctx.channel())
                    ctx.channel().attr<Long>(AK_SESSION_ID).set(sessionId)
                }
                val tunnelId = descriptor.sessionPool.tunnelId
                val head = ctx.alloc().long2Bytes(tunnelId, sessionId)
                descriptor.sessionPool.tunnelChannel.writeAndFlush(LTMassage(LTCommand.REMOTE_CONNECTED, head))
            } else {
                ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
            }
        }
        super.channelActive(ctx)

    }

    override fun channelInactive(ctx: ChannelHandlerContext?) {
        if (ctx != null) {
            val sa = ctx.channel().localAddress() as InetSocketAddress
            val descriptor = registry.getDescriptorByPort(sa.port)
            if (descriptor != null) {
                val sessionId = ctx.channel().attr<Long>(AK_SESSION_ID).get()
                if (sessionId != null) {
                    descriptor.sessionPool.removeChannel(sessionId)
                        ?.writeAndFlush(Unpooled.EMPTY_BUFFER)
                        ?.addListener(ChannelFutureListener.CLOSE)
                }
                // 解决 HTTP/1.x 数据传输问题
                ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener {
                    val tunnelId = descriptor.sessionPool.tunnelId
                    val head = ctx.alloc().long2Bytes(tunnelId, sessionId)
                    descriptor.sessionPool.tunnelChannel.writeAndFlush(LTMassage(LTCommand.REMOTE_CONNECTED, head))
                }
            }
            ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
        }
        super.channelInactive(ctx)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
        logger.trace("exceptionCaught: {}", ctx, cause)
        ctx?.channel()?.writeAndFlush(Unpooled.EMPTY_BUFFER)?.addListener(ChannelFutureListener.CLOSE)
    }

    override fun channelRead0(ctx: ChannelHandlerContext?, msg: ByteBuf?) {
        logger.trace("channelRead0: {}", ctx)
        ctx ?: return
        msg ?: return
        val sessionId = ctx.channel().attr<Long>(AK_SESSION_ID).get() ?: return
        val sa = ctx.channel().localAddress() as InetSocketAddress
        val descriptor = registry.getDescriptorByPort(sa.port) ?: return
        val tunnelId = descriptor.sessionPool.tunnelId
        val head = ctx.alloc().long2Bytes(tunnelId, sessionId)
        val data = msg.toBytes()
        descriptor.sessionPool.tunnelChannel.writeAndFlush(LTMassage(LTCommand.TRANSFER, head, data))
    }

}