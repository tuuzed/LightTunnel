package lighttunnel.server.http

import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
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
import lighttunnel.server.util.AK_IS_INTERCEPTOR_HANDLE
import lighttunnel.server.util.AK_IS_PLUGIN_HANDLE
import lighttunnel.server.util.AK_SESSION_ID

internal class HttpTunnelChannelHandler(
    private val registry: HttpRegistry,
    private val httpPlugin: HttpPlugin? = null,
    private val httpTunnelRequestInterceptor: HttpTunnelRequestInterceptor? = null
) : ChannelInboundHandlerAdapter() {
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

    @Suppress("DuplicatedCode")
    @Throws(Exception::class)
    override fun channelRead(ctx: ChannelHandlerContext?, msg: Any?) {
        logger.trace("channelRead0: {}", ctx)
        ctx ?: return
        msg ?: return
        when (msg) {
            is HttpRequest -> {
                // 插件处理
                val isPluginHandle = httpPlugin?.doHttpRequest(ctx, msg)
                ctx.channel().attr(AK_IS_PLUGIN_HANDLE).set(isPluginHandle)
                if (isPluginHandle == true) {
                    return
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
                    writeNotRegisteredTunnelHttpResponse(ctx, httpHost)
                    return
                }
                ctx.channel().attr(AK_HTTP_HOST).set(httpHost)
                // 拦截器处理
                val isInterceptorHandle = httpTunnelRequestInterceptor?.doHttpRequest(ctx, msg, httpFd.tunnelRequest)
                ctx.channel().attr(AK_IS_INTERCEPTOR_HANDLE).set(isInterceptorHandle)
                if (isPluginHandle == true) {
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
            is HttpContent -> {
                // 插件处理
                val isPluginHandle = ctx.channel().attr(AK_IS_PLUGIN_HANDLE).get()
                if (isPluginHandle == true) {
                    httpPlugin?.doHttpContent(ctx, msg)
                    return
                }
                // 获取Http请求中的域名
                val httpHost = ctx.channel().attr(AK_HTTP_HOST).get()
                if (httpHost == null) {
                    ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
                    return
                }
                // 是否注册过隧道
                val httpFd = registry.getHttpFd(httpHost)
                if (httpFd == null) {
                    writeNotRegisteredTunnelHttpResponse(ctx, httpHost)
                    return
                }
                // 拦截器处理
                val isInterceptorHandle = ctx.channel().attr(AK_IS_INTERCEPTOR_HANDLE).get()
                if (isInterceptorHandle == true) {
                    return
                }
                val sessionId = ctx.channel().attr(AK_SESSION_ID).get()
                if (sessionId == null) {
                    ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
                    return
                }
                httpFd.tunnelChannel.writeAndFlush(
                    ProtoMessage.REMOTE_CONNECTED(httpFd.tunnelId, sessionId, RemoteConnection(ctx.channel().remoteAddress()))
                )
                val data = ByteBufUtil.getBytes(msg.content() ?: Unpooled.EMPTY_BUFFER) ?: emptyBytes
                httpFd.tunnelChannel.writeAndFlush(ProtoMessage.TRANSFER(httpFd.tunnelId, sessionId, data))
            }
        }
    }


    private fun writeNotRegisteredTunnelHttpResponse(ctx: ChannelHandlerContext, httpHost: String) {
        val content = Unpooled.copiedBuffer("隧道（$httpHost）没有注册！", CharsetUtil.UTF_8)
        ctx.write(DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN).apply {
            headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=utf-8")
            headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes())
        })
        ctx.write(DefaultHttpContent(Unpooled.wrappedBuffer(content)))
        ctx.writeAndFlush(DefaultLastHttpContent()).addListener(ChannelFutureListener.CLOSE)
    }

}