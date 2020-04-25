package lighttunnel.server.http

import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.HttpContent
import io.netty.handler.codec.http.HttpRequest
import lighttunnel.logger.loggerDelegate
import lighttunnel.proto.ProtoMessage
import lighttunnel.proto.ProtoMessageType
import lighttunnel.server.interceptor.HttpRequestInterceptor
import lighttunnel.server.util.AttributeKeys
import lighttunnel.util.LongUtil
import lighttunnel.util.http.domainHost
import lighttunnel.util.http.toByteBuf
import lighttunnel.util.http.toBytes

class HttpServerChannelHandler(
    private val registry: HttpRegistry,
    private val interceptor: HttpRequestInterceptor,
    private val httpPlugin: HttpPlugin? = null
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
            val httpFd = registry.getHttpFd(host)
            if (httpFd != null) {
                val head = LongUtil.toBytes(httpFd.tunnelId, sessionId)
                httpFd.tunnelChannel.writeAndFlush(ProtoMessage(ProtoMessageType.REMOTE_DISCONNECT, head))
            }
            ctx.channel().attr(AttributeKeys.AK_HTTP_HOST).set(null)
            ctx.channel().attr(AttributeKeys.AK_SESSION_ID).set(null)
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
            val response = httpPlugin?.doHandle(msg)
            if (response != null) {
                ctx.channel().writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
                ctx.channel().attr(AttributeKeys.AK_HTTP_SKIP).set(false)
            } else {
                doChannelReadHttpRequest(ctx, msg)
            }
        } else if (msg is HttpContent) {
            doChannelReadHttpContent(ctx, msg)
        }
    }

    /** 处理读取到的HttpRequest类型的消息 */
    @Throws(Exception::class)
    private fun doChannelReadHttpRequest(ctx: ChannelHandlerContext, msg: HttpRequest) {
        val host = msg.domainHost
        if (host == null) {
            ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
            return
        }
        ctx.channel().attr(AttributeKeys.AK_HTTP_HOST).set(host)
        val httpFd = registry.getHttpFd(host)
        if (httpFd == null) {
            ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
            ctx.channel().attr(AttributeKeys.AK_HTTP_SKIP).set(false)
            return
        }
        ctx.channel().attr(AttributeKeys.AK_HTTP_SKIP).set(true)
        val httpResponse = interceptor.handleHttpRequest(
            ctx.channel().localAddress(),
            ctx.channel().remoteAddress(),
            httpFd.tunnelRequest,
            msg
        )
        if (httpResponse != null) {
            ctx.channel().writeAndFlush(httpResponse.toByteBuf())
            return
        }
        val sessionId = httpFd.sessionChannels.putChannel(ctx.channel())
        ctx.channel().attr(AttributeKeys.AK_SESSION_ID).set(sessionId)
        val head = LongUtil.toBytes(httpFd.tunnelId, sessionId)
        val data = msg.toBytes()
        httpFd.tunnelChannel.writeAndFlush(ProtoMessage(ProtoMessageType.TRANSFER, head, data))
    }

    /** 处理读取到的HttpContent类型的消息 */
    @Throws(Exception::class)
    private fun doChannelReadHttpContent(ctx: ChannelHandlerContext, msg: HttpContent) {
        val skip = ctx.channel().attr(AttributeKeys.AK_HTTP_SKIP).get() ?: return
        if (!skip) {
            return
        }
        val host = ctx.channel().attr(AttributeKeys.AK_HTTP_HOST).get()
        val sessionId = ctx.channel().attr(AttributeKeys.AK_SESSION_ID).get()
        if (host == null || sessionId == null) {
            ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
            return
        }
        val httpFd = registry.getHttpFd(host)
        if (httpFd == null) {
            ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
            return
        }
        val head = LongUtil.toBytes(httpFd.tunnelId, sessionId)
        val data = ByteBufUtil.getBytes(msg.content())
        httpFd.sessionChannels.tunnelChannel.writeAndFlush(ProtoMessage(ProtoMessageType.TRANSFER, head, data))

    }

}