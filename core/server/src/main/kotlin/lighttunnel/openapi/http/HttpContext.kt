package lighttunnel.openapi.http

import io.netty.channel.ChannelFutureListener
import io.netty.handler.codec.http.HttpContent
import io.netty.handler.codec.http.HttpResponse
import io.netty.util.Attribute
import io.netty.util.AttributeKey
import java.net.SocketAddress

interface HttpContext {
    val localAddress: SocketAddress?
    val remoteAddress: SocketAddress?
    fun <T> attr(key: AttributeKey<T>): Attribute<T>?
    fun writeHttpResponse(response: HttpResponse, flush: Boolean = false, listener: ChannelFutureListener? = null)
    fun writeHttpContent(response: HttpContent, flush: Boolean = false, listener: ChannelFutureListener? = null)
}