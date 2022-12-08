package lighttunnel.server.http

import io.netty.buffer.ByteBufUtil
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.HttpContent
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponseStatus
import lighttunnel.common.entity.RemoteConn
import lighttunnel.common.proto.msg.ProtoMsgRemoteConnected
import lighttunnel.common.proto.msg.ProtoMsgRemoteDisconnect
import lighttunnel.common.proto.msg.ProtoMsgTransfer
import lighttunnel.common.utils.*
import lighttunnel.server.utils.*

internal class HttpTunnelChannelHandler(
    private val registry: HttpRegistry,
    private val httpPlugin: HttpPlugin? = null,
    private val httpTunnelRequestInterceptor: HttpTunnelRequestInterceptor? = null
) : ChannelInboundHandlerAdapter() {
    private val logger by injectLogger()

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
            val descriptor = registry.getHttpDescriptor(httpHost)
            val tunnelChannel = descriptor?.tunnelChannel
            if (tunnelChannel != null) {
                val aes128Key = tunnelChannel.attr(AK_AES128_KEY).get()
                val compressedAndData =
                    (RemoteConn(ctx.channel().remoteAddress()).asJsonString()?.toByteArray() ?: emptyBytes)
                        .tryGZip()
                        .let {
                            it.first to if (it.second.isNotEmpty() && aes128Key != null) it.second.tryEncryptAES128(aes128Key) else it.second
                        }
                tunnelChannel.writeAndFlush(
                    ProtoMsgRemoteDisconnect(
                        descriptor.tunnelId,
                        sessionId,
                        compressedAndData.second,
                        aes128Key,
                        compressedAndData.first,
                    )
                )
            }
            ctx.channel().attr(AK_HTTP_HOST).set(null)
            ctx.channel().attr(AK_SESSION_ID).set(null)
        }
        super.channelInactive(ctx)
    }

    @Throws(Exception::class)
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable?) {
        logger.trace("exceptionCaught: {}", ctx, cause)
        ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
    }

    @Suppress("DuplicatedCode")
    @Throws(Exception::class)
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        logger.trace("channelRead0: {}, {}", ctx, msg)
        val httpContext = ctx.channel().attr(AK_HTTP_CONTEXT).get() ?: DefaultHttpContext(ctx).also {
            ctx.channel().attr(AK_HTTP_CONTEXT).set(it)
        }
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
                val descriptor = registry.getHttpDescriptor(httpHost)
                if (descriptor == null) {
                    httpContext.writeTextHttpResponse(
                        status = HttpResponseStatus.FORBIDDEN, text = "Tunnel($httpHost)Not Registered!"
                    )
                    return
                }
                // 拦截器处理
                ctx.channel().attr(AK_IS_INTERCEPTOR_HANDLE)
                    .set(httpTunnelRequestInterceptor?.doHttpRequest(httpContext, msg, descriptor.tunnelRequest))
                if (ctx.isInterceptorHandle) {
                    return
                }
                val sessionId = descriptor.putSessionChannel(ctx.channel())
                ctx.channel().attr(AK_SESSION_ID).set(sessionId)
                val aes128Key = descriptor.tunnelChannel.attr(AK_AES128_KEY).get()
                val compressedAndData1 =
                    (RemoteConn(ctx.channel().remoteAddress()).asJsonString()?.toByteArray() ?: emptyBytes)
                        .tryGZip()
                        .let {
                            it.first to if (it.second.isNotEmpty() && aes128Key != null) it.second.tryEncryptAES128(aes128Key) else it.second
                        }
                descriptor.tunnelChannel.writeAndFlush(
                    ProtoMsgRemoteConnected(
                        descriptor.tunnelId,
                        sessionId,
                        compressedAndData1.second,
                        aes128Key,
                        compressedAndData1.first,
                    )
                )
                val compressedAndData2 = (ByteBufUtil.getBytes(msg.asByteBuf) ?: emptyBytes)
                    .tryGZip()
                    .let {
                        it.first to if (it.second.isNotEmpty() && aes128Key != null) it.second.tryEncryptAES128(aes128Key) else it.second
                    }
                descriptor.tunnelChannel.writeAndFlush(
                    ProtoMsgTransfer(
                        descriptor.tunnelId,
                        sessionId,
                        compressedAndData2.second,
                        aes128Key,
                        compressedAndData2.first,
                    )
                )
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
                val descriptor = registry.getHttpDescriptor(httpHost) ?: return
                // 拦截器处理
                if (ctx.isInterceptorHandle) {
                    httpTunnelRequestInterceptor?.doHttpContent(httpContext, msg, descriptor.tunnelRequest)
                    return
                }
                val sessionId = ctx.channel().attr(AK_SESSION_ID).get()
                if (sessionId == null) {
                    ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
                    return
                }
                val aes128Key = descriptor.tunnelChannel.attr(AK_AES128_KEY).get()
                val compressedAndData1 =
                    (RemoteConn(ctx.channel().remoteAddress()).asJsonString()?.toByteArray() ?: emptyBytes)
                        .tryGZip()
                        .let {
                            it.first to if (it.second.isNotEmpty() && aes128Key != null) it.second.tryEncryptAES128(aes128Key) else it.second
                        }
                descriptor.tunnelChannel.writeAndFlush(
                    ProtoMsgRemoteConnected(
                        descriptor.tunnelId,
                        sessionId,
                        compressedAndData1.second,
                        aes128Key,
                        compressedAndData1.first,
                    )
                )
                val compressedAndData2 =
                    (ByteBufUtil.getBytes(msg.content() ?: Unpooled.EMPTY_BUFFER) ?: emptyBytes)
                        .tryGZip()
                        .let {
                            it.first to if (it.second.isNotEmpty() && aes128Key != null) it.second.tryEncryptAES128(aes128Key) else it.second
                        }
                descriptor.tunnelChannel.writeAndFlush(
                    ProtoMsgTransfer(
                        descriptor.tunnelId,
                        sessionId,
                        compressedAndData2.second,
                        aes128Key,
                        compressedAndData2.first,
                    )
                )
            }
        }
    }

    private val ChannelHandlerContext.isPluginHandle
        get() = this.channel().attr(AK_IS_PLUGIN_HANDLE).get() == true

    private val ChannelHandlerContext.isInterceptorHandle
        get() = this.channel().attr(AK_IS_INTERCEPTOR_HANDLE).get() == true

}
