package lighttunnel.server.http

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
import io.netty.util.Attribute
import io.netty.util.AttributeKey
import lighttunnel.base.utils.asByteBuf
import java.net.SocketAddress

sealed interface HttpContext {
    val localAddress: SocketAddress?
    val remoteAddress: SocketAddress?
    fun <T> attr(key: AttributeKey<T>): Attribute<T>?
    fun write(response: HttpResponse, flush: Boolean = false, listener: ChannelFutureListener? = null)
    fun write(content: HttpContent, flush: Boolean = false, listener: ChannelFutureListener? = null)
}

internal class DefaultHttpContext(
    private val ctx: ChannelHandlerContext
) : HttpContext {

    override val localAddress: SocketAddress? = ctx.channel().localAddress()

    override val remoteAddress: SocketAddress? = ctx.channel().remoteAddress()

    override fun <T> attr(key: AttributeKey<T>): Attribute<T>? = ctx.channel().attr(key)

    override fun write(response: HttpResponse, flush: Boolean, listener: ChannelFutureListener?) {
        val channelFuture = if (flush) {
            ctx.write(response.asByteBuf)
        } else {
            ctx.writeAndFlush(response.asByteBuf)
        }
        if (listener != null) {
            channelFuture.addListener(listener)
        }
    }

    override fun write(content: HttpContent, flush: Boolean, listener: ChannelFutureListener?) {
        val channelFuture = if (flush) {
            ctx.write(content.asByteBuf)
        } else {
            ctx.writeAndFlush(content.asByteBuf)
        }
        if (listener != null) {
            channelFuture.addListener(listener)
        }
    }

    fun writeTextHttpResponse(status: HttpResponseStatus = HttpResponseStatus.OK, text: String = status.toString()) {
        val content = Unpooled.copiedBuffer(text, Charsets.UTF_8) ?: Unpooled.EMPTY_BUFFER
        write(
            DefaultHttpResponse(HttpVersion.HTTP_1_1, status).apply {
                headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=utf-8")
                headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes())
            }
        )
        write(DefaultHttpContent(content))
        write(LastHttpContent.EMPTY_LAST_CONTENT, flush = true, listener = ChannelFutureListener.CLOSE)
    }

}
