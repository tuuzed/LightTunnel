package lighttunnel.server.http

import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.*
import lighttunnel.base.proto.ProtoMessage
import lighttunnel.base.proto.emptyBytes
import lighttunnel.base.util.byteBuf
import lighttunnel.base.util.hostExcludePort
import lighttunnel.base.util.loggerDelegate
import lighttunnel.openapi.RemoteConnection
import lighttunnel.openapi.http.HttpChain
import lighttunnel.openapi.http.HttpPlugin
import lighttunnel.openapi.http.HttpTunnelRequestInterceptor
import lighttunnel.server.util.*

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
        val chain = ctx.channel().attr(AK_HTTP_CHAIN).get()
            ?: HttpChainDefaultImpl(ctx).also { ctx.channel().attr(AK_HTTP_CHAIN).set(it) }
        when (msg) {
            is HttpRequest -> {
                // 插件处理
                ctx.channel().attr(AK_IS_PLUGIN_HANDLE).set(httpPlugin?.doHttpRequest(chain, msg))
                if (ctx.isPluginHandle) {
                    return
                }
                // 获取Http请求中的域名
                val httpHost = msg.hostExcludePort
                if (httpHost == null) {
                    writeSimpleHttpResponse(chain, status = HttpResponseStatus.BAD_REQUEST)
                    return
                }
                ctx.channel().attr(AK_HTTP_HOST).set(httpHost)
                // 是否注册过隧道
                val httpFd = registry.getHttpFd(httpHost)
                if (httpFd == null) {
                    writeSimpleHttpResponse(chain, status = HttpResponseStatus.FORBIDDEN, content = "Tunnel（$httpHost）Not Registered!")
                    return
                }
                // 拦截器处理
                ctx.channel().attr(AK_IS_INTERCEPTOR_HANDLE).set(httpTunnelRequestInterceptor?.doHttpRequest(chain, msg, httpFd.tunnelRequest))
                if (ctx.isInterceptorHandle) {
                    return
                }
                val sessionId = httpFd.putChannel(ctx.channel())
                ctx.channel().attr(AK_SESSION_ID).set(sessionId)
                httpFd.tunnelChannel.writeAndFlush(
                    ProtoMessage.REMOTE_CONNECTED(httpFd.tunnelId, sessionId, RemoteConnection(ctx.channel().remoteAddress()))
                )
                val data = ByteBufUtil.getBytes(msg.byteBuf) ?: emptyBytes
                httpFd.tunnelChannel.writeAndFlush(ProtoMessage.TRANSFER(httpFd.tunnelId, sessionId, data))
            }
            is HttpContent -> {
                // 插件处理
                if (ctx.isPluginHandle) {
                    httpPlugin?.doHttpContent(chain, msg)
                    return
                }
                // 获取Http请求中的域名
                val httpHost = ctx.channel().attr(AK_HTTP_HOST).get()
                if (httpHost == null) {
                    writeSimpleHttpResponse(chain, status = HttpResponseStatus.BAD_REQUEST)
                    return
                }
                // 是否注册过隧道
                val httpFd = registry.getHttpFd(httpHost) ?: return
                // 拦截器处理
                if (ctx.isInterceptorHandle) {
                    httpTunnelRequestInterceptor?.doHttpContent(chain, msg, httpFd.tunnelRequest)
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
                httpFd.tunnelChannel.writeAndFlush(
                    ProtoMessage.TRANSFER(httpFd.tunnelId, sessionId, data)
                )
            }
        }
    }

    private val ChannelHandlerContext.isPluginHandle get() = this.channel().attr(AK_IS_PLUGIN_HANDLE).get() == true
    private val ChannelHandlerContext.isInterceptorHandle get() = this.channel().attr(AK_IS_INTERCEPTOR_HANDLE).get() == true

    private fun writeSimpleHttpResponse(
        chain: HttpChain,
        status: HttpResponseStatus = HttpResponseStatus.OK,
        content: String = status.toString()
    ) {
        val contentByteBuf = Unpooled.copiedBuffer(content, Charsets.UTF_8)
        chain.writeHttpResponse(
            DefaultHttpResponse(HttpVersion.HTTP_1_1, status).apply {
                headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=utf-8")
                headers().set(HttpHeaderNames.CONTENT_LENGTH, contentByteBuf.readableBytes())
            }
        )
        chain.writeHttpContent(DefaultHttpContent(contentByteBuf))
        chain.writeHttpContent(LastHttpContent.EMPTY_LAST_CONTENT, flush = true, listener = ChannelFutureListener.CLOSE)
    }

}