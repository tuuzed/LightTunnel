package lighttunnel.server.http

import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.FullHttpRequest
import lighttunnel.logger.loggerDelegate
import lighttunnel.proto.ProtoMessage
import lighttunnel.proto.ProtoMessageType
import lighttunnel.server.util.AttributeKeys
import lighttunnel.util.HttpUtil
import lighttunnel.util.LongUtil

internal class HttpServerChannelHandler(
    private val registry: HttpRegistry,
    private val interceptor: HttpRequestInterceptor,
    private val httpPlugin: HttpPlugin? = null
) : SimpleChannelInboundHandler<FullHttpRequest>() {
    private val logger by loggerDelegate()

    @Throws(Exception::class)
    override fun channelActive(ctx: ChannelHandlerContext?) {
        logger.trace("channelActive: {}", ctx)
        super.channelActive(ctx)
    }

    @Throws(Exception::class)
    override fun channelInactive(ctx: ChannelHandlerContext?) {
        logger.trace("channelInactive: {}", ctx)
        if (ctx != null) {
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
        }
        super.channelInactive(ctx)
    }

    @Throws(Exception::class)
    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
        logger.trace("exceptionCaught: {}", ctx, cause)
        ctx ?: return
        ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
    }

    override fun channelRead0(ctx: ChannelHandlerContext?, msg: FullHttpRequest?) {
        logger.trace("channelRead0: {}", ctx)
        ctx ?: return
        msg ?: return
        val httpPluginResponse = httpPlugin?.doHandle(msg)
        if (httpPluginResponse != null) {
            ctx.channel().writeAndFlush(HttpUtil.toByteBuf(httpPluginResponse)).addListener(ChannelFutureListener.CLOSE)
        }
        val host = HttpUtil.getHostWithoutPort(msg)
        if (host == null) {
            ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
            return
        }
        ctx.channel().attr(AttributeKeys.AK_HTTP_HOST).set(host)
        val httpFd = registry.getHttpFd(host)
        if (httpFd == null) {
            ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
            return
        }
        val httpInterceptorResponse = interceptor.handleHttpRequest(ctx, httpFd.tunnelRequest, msg)
        if (httpInterceptorResponse != null) {
            ctx.channel().writeAndFlush(HttpUtil.toByteBuf(httpInterceptorResponse))
            return
        }
        val sessionId = httpFd.sessionChannels.putChannel(ctx.channel())
        ctx.channel().attr(AttributeKeys.AK_SESSION_ID).set(sessionId)
        val head = LongUtil.toBytes(httpFd.tunnelId, sessionId)
        val data = ByteBufUtil.getBytes(HttpUtil.toByteBuf(msg))
        httpFd.tunnelChannel.writeAndFlush(ProtoMessage(ProtoMessageType.TRANSFER, head, data))
    }

}