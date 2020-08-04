package lighttunnel.server.http

import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.*
import io.netty.util.CharsetUtil
import lighttunnel.base.proto.ProtoMessage
import lighttunnel.base.util.HttpUtil
import lighttunnel.base.util.emptyBytes
import lighttunnel.base.util.loggerDelegate
import lighttunnel.openapi.RemoteConnection
import lighttunnel.openapi.http.HttpPlugin
import lighttunnel.openapi.http.HttpTunnelRequestInterceptor
import lighttunnel.server.util.AK_HTTP_HOST
import lighttunnel.server.util.AK_SESSION_ID

internal class HttpTunnelChannelHandler(
    private val registry: HttpRegistry,
    private val httpPlugin: HttpPlugin? = null,
    private val httpTunnelRequestInterceptor: HttpTunnelRequestInterceptor? = null
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
        if (ctx == null) {
            super.channelInactive(ctx)
            return
        }
        val httpHost = ctx.channel().attr(AK_HTTP_HOST).get()
        val sessionId = ctx.channel().attr(AK_SESSION_ID).get()
        if (httpHost != null && sessionId != null) {
            val httpFd = registry.getHttpFd(httpHost)
            httpFd?.tunnelChannel?.writeAndFlush(
                ProtoMessage.REMOTE_DISCONNECT(httpFd.tunnelId, sessionId, RemoteConnection(ctx.channel().remoteAddress()))
            )
            ctx.channel().attr(AK_HTTP_HOST).set(null)
            ctx.channel().attr(AK_SESSION_ID).set(null)
        }
        super.channelInactive(ctx)
    }

    @Throws(Exception::class)
    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
        logger.trace("exceptionCaught: {}", ctx, cause)
        ctx ?: return
        ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
    }

    @Throws(Exception::class)
    override fun channelRead0(ctx: ChannelHandlerContext?, msg: FullHttpRequest?) {
        logger.trace("channelRead0: {}", ctx)
        ctx ?: return
        msg ?: return
        // 插件处理
        val httpPluginResponse = httpPlugin?.doHandle(msg)
        if (httpPluginResponse != null) {
            ctx.channel().writeAndFlush(HttpUtil.toByteBuf(httpPluginResponse)).addListener(ChannelFutureListener.CLOSE)
        }
        // 获取Http请求中的域名
        val httpHost = HttpUtil.getHostWithoutPort(msg)
        if (httpHost == null) {
            ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
            return
        }
        // 是否注册过隧道
        val httpFd = registry.getHttpFd(httpHost)
        if (httpFd == null) {
            val notRegisteredTunnelHttpResponse = newNotRegisteredTunnelHttpResponse(httpHost)
            ctx.channel().writeAndFlush(HttpUtil.toByteBuf(notRegisteredTunnelHttpResponse)).addListener(ChannelFutureListener.CLOSE)
            return
        }
        ctx.channel().attr(AK_HTTP_HOST).set(httpHost)
        // 拦截器
        val httpInterceptorResponse = httpTunnelRequestInterceptor?.intercept(ctx, httpFd.tunnelRequest, msg)
        if (httpInterceptorResponse != null) {
            ctx.channel().writeAndFlush(HttpUtil.toByteBuf(httpInterceptorResponse))
            return
        }
        val sessionId = httpFd.putChannel(ctx.channel())
        ctx.channel().attr(AK_SESSION_ID).set(sessionId)
        httpFd.tunnelChannel.writeAndFlush(
            ProtoMessage.REMOTE_CONNECTED(httpFd.tunnelId, sessionId, RemoteConnection(ctx.channel().remoteAddress()))
        )
        val data = ByteBufUtil.getBytes(HttpUtil.toByteBuf(msg)) ?: emptyBytes
        httpFd.tunnelChannel.writeAndFlush(ProtoMessage.TRANSFER(httpFd.tunnelId, sessionId, data))
    }

    private fun newNotRegisteredTunnelHttpResponse(httpHost: String): HttpResponse {
        val content = Unpooled.copiedBuffer("隧道（$httpHost）没有注册！", CharsetUtil.UTF_8)
        return DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            HttpResponseStatus.FORBIDDEN,
            content
        ).also {
            it.headers()
                .set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=utf-8")
                .set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes())
        }
    }

}