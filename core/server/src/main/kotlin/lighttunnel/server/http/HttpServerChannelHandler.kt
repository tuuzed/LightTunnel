package lighttunnel.server.http

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.HttpContent
import io.netty.handler.codec.http.HttpRequest
import lighttunnel.logging.loggerDelegate
import lighttunnel.proto.ProtoCommand
import lighttunnel.proto.ProtoMassage
import lighttunnel.server.util.host
import lighttunnel.server.util.toByteBuf
import lighttunnel.server.util.toBytes
import lighttunnel.server.util.AttrKeys
import lighttunnel.util.long2Bytes
import lighttunnel.util.toBytes


class HttpServerChannelHandler(
    private val registry: HttpRegistry,
    private val interceptor: HttpRequestInterceptor
) : ChannelInboundHandlerAdapter() {

    private val logger by loggerDelegate()

    @Throws(Exception::class)
    override fun channelActive(ctx: ChannelHandlerContext) {
        super.channelActive(ctx)
        logger.trace("channelActive: {}", ctx)
    }

    @Throws(Exception::class)
    override fun channelInactive(ctx: ChannelHandlerContext) {
        logger.trace("channelInactive: {}", ctx)
        val host = ctx.channel().attr(AttrKeys.AK_HTTP_HOST).get()
        val sessionId = ctx.channel().attr(AttrKeys.AK_SESSION_ID).get()
        if (host != null && sessionId != null) {
            val descriptor = registry.getDescriptorByHost(host)
            if (descriptor != null) {
                val tunnelId = descriptor.sessionPool.tunnelId
                val head = ctx.alloc().long2Bytes(tunnelId, sessionId)
                descriptor.sessionPool.tunnelChannel.writeAndFlush(ProtoMassage(ProtoCommand.REMOTE_DISCONNECT, head))
            }
            ctx.channel().attr<String>(AttrKeys.AK_HTTP_HOST).set(null)
            ctx.channel().attr<Long>(AttrKeys.AK_SESSION_ID).set(null)
        }
        super.channelInactive(ctx)
        ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
    }

    @Throws(Exception::class)
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        logger.trace("exceptionCaught: {}", ctx, cause)
        ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
    }

    @Throws(Exception::class)
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is HttpRequest) {
            channelReadHttpRequest(ctx, msg)
        } else if (msg is HttpContent) {
            channelReadHttpContent(ctx, msg)
        }
    }

    /**处理读取到的HttpRequest类型的消息 */
    @Throws(Exception::class)
    private fun channelReadHttpRequest(ctx: ChannelHandlerContext, msg: HttpRequest) {
        val host = msg.host()
        if (host == null) {
            ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
            return
        }
        ctx.channel().attr(AttrKeys.AK_HTTP_HOST).set(host)
        val descriptor = registry.getDescriptorByHost(host)
        if (descriptor == null) {
            ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
            ctx.channel().attr<Boolean>(AttrKeys.AK_HTTP_SKIP).set(false)
            return
        }
        ctx.channel().attr(AttrKeys.AK_HTTP_SKIP).set(true)
        val httpResponse = interceptor.handleHttpRequest(
            ctx.channel().localAddress(),
            ctx.channel().remoteAddress(),
            descriptor.sessionPool.request,
            msg
        )
        if (httpResponse != null) {
            ctx.channel().writeAndFlush(httpResponse.toByteBuf())
            return
        }
        val sessionId = descriptor.sessionPool.putChannel(ctx.channel())
        ctx.channel().attr(AttrKeys.AK_SESSION_ID).set(sessionId)
        val tunnelId = descriptor.sessionPool.tunnelId
        val head = ctx.alloc().long2Bytes(tunnelId, sessionId)
        val data = msg.toBytes()
        descriptor.sessionPool.tunnelChannel.writeAndFlush(ProtoMassage(ProtoCommand.TRANSFER, head, data))
    }

    /** 处理读取到的HttpContent类型的消息 */
    @Throws(Exception::class)
    private fun channelReadHttpContent(ctx: ChannelHandlerContext, msg: HttpContent) {
        val skip = ctx.channel().attr(AttrKeys.AK_HTTP_SKIP).get() ?: return
        if (!skip) return
        val host = ctx.channel().attr(AttrKeys.AK_HTTP_HOST).get()
        val sessionId = ctx.channel().attr(AttrKeys.AK_SESSION_ID).get()
        if (host == null || sessionId == null) {
            ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
            return
        }
        val descriptor = registry.getDescriptorByHost(host)
        if (descriptor == null) {
            ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
            return
        }
        val tunnelId = descriptor.sessionPool.tunnelId
        val head = ctx.alloc().long2Bytes(tunnelId, sessionId)
        val data = msg.content().toBytes()
        descriptor.sessionPool.tunnelChannel.writeAndFlush(ProtoMassage(ProtoCommand.TRANSFER, head, data))

    }

}