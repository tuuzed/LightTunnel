package lighttunnel.server.http

import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.HttpContent
import io.netty.handler.codec.http.HttpRequest
import lighttunnel.logger.loggerDelegate
import lighttunnel.proto.ProtoCommand
import lighttunnel.proto.ProtoMessage
import lighttunnel.server.interceptor.HttpRequestInterceptor
import lighttunnel.server.util.AttributeKeys
import lighttunnel.server.util.HttpUtil
import lighttunnel.util.LongUtil

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
        val host = ctx.channel().attr(AttributeKeys.AK_HTTP_HOST).get()
        val sessionId = ctx.channel().attr(AttributeKeys.AK_SESSION_ID).get()
        if (host != null && sessionId != null) {
            val descriptor = registry.getDescriptor(host)
            if (descriptor != null) {
                val tunnelId = descriptor.sessionChannels.tunnelId
                val head = LongUtil.toBytes(tunnelId, sessionId)
                descriptor.sessionChannels.tunnelChannel.writeAndFlush(ProtoMessage(ProtoCommand.REMOTE_DISCONNECT, head))
            }
            ctx.channel().attr<String>(AttributeKeys.AK_HTTP_HOST).set(null)
            ctx.channel().attr<Long>(AttributeKeys.AK_SESSION_ID).set(null)
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
        val host = HttpUtil.getHost(msg)
        if (host == null) {
            ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
            return
        }
        ctx.channel().attr(AttributeKeys.AK_HTTP_HOST).set(host)
        val descriptor = registry.getDescriptor(host)
        if (descriptor == null) {
            ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
            ctx.channel().attr<Boolean>(AttributeKeys.AK_HTTP_SKIP).set(false)
            return
        }
        ctx.channel().attr(AttributeKeys.AK_HTTP_SKIP).set(true)
        val httpResponse = interceptor.handleHttpRequest(
            ctx.channel().localAddress(),
            ctx.channel().remoteAddress(),
            descriptor.sessionChannels.tunnelRequest,
            msg
        )
        if (httpResponse != null) {
            ctx.channel().writeAndFlush(HttpUtil.toByteBuf(httpResponse))
            return
        }
        val sessionId = descriptor.sessionChannels.putChannel(ctx.channel())
        ctx.channel().attr(AttributeKeys.AK_SESSION_ID).set(sessionId)
        val tunnelId = descriptor.sessionChannels.tunnelId
        val head = LongUtil.toBytes(tunnelId, sessionId)
        val data = HttpUtil.toBytes(msg)
        descriptor.sessionChannels.tunnelChannel.writeAndFlush(ProtoMessage(ProtoCommand.TRANSFER, head, data))
    }

    /** 处理读取到的HttpContent类型的消息 */
    @Throws(Exception::class)
    private fun channelReadHttpContent(ctx: ChannelHandlerContext, msg: HttpContent) {
        val skip = ctx.channel().attr(AttributeKeys.AK_HTTP_SKIP).get() ?: return
        if (!skip) return
        val host = ctx.channel().attr(AttributeKeys.AK_HTTP_HOST).get()
        val sessionId = ctx.channel().attr(AttributeKeys.AK_SESSION_ID).get()
        if (host == null || sessionId == null) {
            ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
            return
        }
        val descriptor = registry.getDescriptor(host)
        if (descriptor == null) {
            ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
            return
        }
        val tunnelId = descriptor.sessionChannels.tunnelId
        val head = LongUtil.toBytes(tunnelId, sessionId)
        val data = ByteBufUtil.getBytes(msg.content())
        descriptor.sessionChannels.tunnelChannel.writeAndFlush(ProtoMessage(ProtoCommand.TRANSFER, head, data))

    }

}