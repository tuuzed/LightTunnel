package lighttunnel.internal.server.http

import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.HttpContent
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponseStatus
import lighttunnel.RemoteConnection
import lighttunnel.http.HttpPlugin
import lighttunnel.http.HttpTunnelRequestInterceptor
import lighttunnel.internal.base.proto.ProtoMessage
import lighttunnel.internal.base.proto.emptyBytes
import lighttunnel.internal.base.util.byteBuf
import lighttunnel.internal.base.util.hostExcludePort
import lighttunnel.internal.base.util.loggerDelegate
import lighttunnel.internal.server.util.*

internal class HttpTunnelChannelHandler(
    private val registry: HttpRegistry,
    private val httpPlugin: HttpPlugin? = null,
    private val httpTunnelRequestInterceptor: HttpTunnelRequestInterceptor? = null
) : ChannelInboundHandlerAdapter() {
    private val logger by loggerDelegate()

    @Throws(Exception::class)
    override fun channelActive(ctx: ChannelHandlerContext) {
        logger.trace("channelActive: {}", ctx)
        super.channelActive(ctx)
    }

    @Throws(Exception::class)
    override fun channelInactive(ctx: ChannelHandlerContext) {
        logger.trace("channelInactive: {}", ctx)
        val httpHost = ctx.channel().attr(AK_HTTP_HOST).get()
        val sessionId = ctx.channel().attr(AK_SESSION_ID).get()
        if (httpHost != null && sessionId != null) {
            val httpFd = registry.getHttpFd(httpHost)
            httpFd?.tunnelChannel?.writeAndFlush(
                ProtoMessage.REMOTE_DISCONNECT(
                    httpFd.tunnelId,
                    sessionId,
                    RemoteConnection(ctx.channel().remoteAddress())
                )
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
    override fun channelRead(ctx: ChannelHandlerContext?, msg: Any?) {
        logger.trace("channelRead0: {}", ctx)
        ctx ?: return
        msg ?: return
        val httpContext = ctx.channel().attr(AK_HTTP_CONTEXT).get()
            ?: HttpContextDefaultImpl(ctx).also { ctx.channel().attr(AK_HTTP_CONTEXT).set(it) }
        when (msg) {
            is HttpRequest -> {
                // 插件处理
                ctx.channel().attr(AK_IS_PLUGIN_HANDLE).set(httpPlugin?.doHttpRequest(httpContext, msg))
                if (ctx.isPluginHandle) {
                    return
                }
                // 获取Http请求中的域名
                val httpHost = msg.hostExcludePort
                if (httpHost == null) {
                    httpContext.writeTextHttpResponse(status = HttpResponseStatus.BAD_GATEWAY)
                    return
                }
                ctx.channel().attr(AK_HTTP_HOST).set(httpHost)
                // 是否注册过隧道
                val httpFd = registry.getHttpFd(httpHost)
                if (httpFd == null) {
                    httpContext.writeTextHttpResponse(
                        status = HttpResponseStatus.FORBIDDEN,
                        text = "Tunnel($httpHost)Not Registered!"
                    )
                    return
                }
                // 拦截器处理
                ctx.channel().attr(AK_IS_INTERCEPTOR_HANDLE)
                    .set(httpTunnelRequestInterceptor?.doHttpRequest(httpContext, msg, httpFd.tunnelRequest))
                if (ctx.isInterceptorHandle) {
                    return
                }
                val sessionId = httpFd.putChannel(ctx.channel())
                ctx.channel().attr(AK_SESSION_ID).set(sessionId)
                httpFd.tunnelChannel.writeAndFlush(
                    ProtoMessage.REMOTE_CONNECTED(
                        httpFd.tunnelId,
                        sessionId,
                        RemoteConnection(ctx.channel().remoteAddress())
                    )
                )
                val data = ByteBufUtil.getBytes(msg.byteBuf) ?: emptyBytes
                httpFd.tunnelChannel.writeAndFlush(ProtoMessage.TRANSFER(httpFd.tunnelId, sessionId, data))
            }
            is HttpContent -> {
                // 插件处理
                if (ctx.isPluginHandle) {
                    httpPlugin?.doHttpContent(httpContext, msg)
                    return
                }
                // 获取Http请求中的域名
                val httpHost = ctx.channel().attr(AK_HTTP_HOST).get()
                if (httpHost == null) {
                    httpContext.writeTextHttpResponse(status = HttpResponseStatus.BAD_GATEWAY)
                    return
                }
                // 是否注册过隧道
                val httpFd = registry.getHttpFd(httpHost) ?: return
                // 拦截器处理
                if (ctx.isInterceptorHandle) {
                    httpTunnelRequestInterceptor?.doHttpContent(httpContext, msg, httpFd.tunnelRequest)
                    return
                }
                val sessionId = ctx.channel().attr(AK_SESSION_ID).get()
                if (sessionId == null) {
                    ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
                    return
                }
                httpFd.tunnelChannel.writeAndFlush(
                    ProtoMessage.REMOTE_CONNECTED(
                        httpFd.tunnelId,
                        sessionId,
                        RemoteConnection(ctx.channel().remoteAddress())
                    )
                )
                val data = ByteBufUtil.getBytes(msg.content() ?: Unpooled.EMPTY_BUFFER) ?: emptyBytes
                httpFd.tunnelChannel.writeAndFlush(
                    ProtoMessage.TRANSFER(httpFd.tunnelId, sessionId, data)
                )
            }
        }
    }

    private val ChannelHandlerContext.isPluginHandle get() = this.channel().attr(AK_IS_PLUGIN_HANDLE).get() == true
    private val ChannelHandlerContext.isInterceptorHandle
        get() = this.channel().attr(AK_IS_INTERCEPTOR_HANDLE).get() == true

}
