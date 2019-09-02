package tunnel2.server.http

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.HttpContent
import io.netty.handler.codec.http.HttpRequest
import tunnel2.common.logging.LoggerFactory
import tunnel2.common.proto.ProtoCw
import tunnel2.common.proto.ProtoMessage
import tunnel2.server.interceptor.HttpRequestInterceptor
import tunnel2.server.internal.*

class HttpServerChannelHandler(
    private val registry: HttpRegistry,
    private val interceptor: HttpRequestInterceptor
) : ChannelInboundHandlerAdapter() {

    companion object {
        private val logger = LoggerFactory.getLogger(HttpServerChannelHandler::class.java)
    }

    @Throws(Exception::class)
    override fun channelActive(ctx: ChannelHandlerContext) {
        super.channelActive(ctx)
        logger.trace("channelActive: {}", ctx)
    }

    @Throws(Exception::class)
    override fun channelInactive(ctx: ChannelHandlerContext) {
        logger.trace("channelInactive: {}", ctx)
        val vhost = ctx.channel().attr<String>(AK_VHOST).get()
        val sessionId = ctx.channel().attr<Long>(AK_SESSION_ID).get()
        if (vhost != null && sessionId != null) {
            registry.getDescriptorByVhost(vhost)?.also {
                it.sessionChannels.tunnelChannel.writeAndFlush(
                    ProtoMessage(
                        ProtoCw.REMOTE_DISCONNECT,
                        it.sessionChannels.tunnelId,
                        sessionId
                    )
                )
            }
            ctx.channel().attr<String>(AK_VHOST).set(null)
            ctx.channel().attr<Long>(AK_SESSION_ID).set(null)
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
        val vhost = msg.vhost()
        if (vhost == null) {
            ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
            return
        }
        ctx.channel().attr<String>(AK_VHOST).set(vhost)
        val descriptor = registry.getDescriptorByVhost(vhost)
        if (descriptor == null) {
            ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
            ctx.channel().attr<Boolean>(AK_PASSED).set(false)
            return
        }
        ctx.channel().attr<Boolean>(AK_PASSED).set(true)
        val httpResponse = interceptor.handleHttpRequest(
            ctx.channel().localAddress(),
            ctx.channel().remoteAddress(),
            descriptor.sessionChannels.tunnelRequest,
            msg
        )
        if (httpResponse != null) {
            ctx.channel().writeAndFlush(httpResponse.toBytes())
            return
        }
        val sessionId = descriptor.sessionChannels.putSessionChannel(ctx.channel())
        ctx.channel().attr<Long>(AK_SESSION_ID).set(sessionId)
        val tunnelId = descriptor.sessionChannels.tunnelId
        val requestBytes = msg.toBytes()
        descriptor.sessionChannels.tunnelChannel.writeAndFlush(
            ProtoMessage(
                ProtoCw.TRANSFER,
                tunnelId,
                sessionId,
                requestBytes
            )
        )
    }

    /** 处理读取到的HttpContent类型的消息 */
    @Throws(Exception::class)
    private fun channelReadHttpContent(ctx: ChannelHandlerContext, msg: HttpContent) {
        val passed = ctx.channel().attr<Boolean>(AK_PASSED).get() ?: return
        if (!passed) { // 如果没有放行
            return
        }
        val vhost = ctx.channel().attr<String>(AK_VHOST).get()
        val sessionId = ctx.channel().attr<Long>(AK_SESSION_ID).get()
        if (vhost == null || sessionId == null) {
            ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
            return
        }
        val descriptor = registry.getDescriptorByVhost(vhost)
        if (descriptor == null) {
            ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
            return
        }

        val tunnelId = descriptor.sessionChannels.tunnelId
        val contentBytes = ByteArray(msg.content().readableBytes())
        msg.content().readBytes(contentBytes)

        descriptor.sessionChannels.tunnelChannel.writeAndFlush(
            ProtoMessage(
                ProtoCw.TRANSFER,
                tunnelId,
                sessionId,
                contentBytes
            )
        )
    }

}