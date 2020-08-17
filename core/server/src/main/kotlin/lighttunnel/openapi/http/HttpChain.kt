package lighttunnel.openapi.http

import io.netty.channel.ChannelFutureListener
import io.netty.handler.codec.http.HttpContent
import io.netty.handler.codec.http.HttpResponse
import java.net.SocketAddress

interface HttpChain {
    val localAddress: SocketAddress?
    val remoteAddress: SocketAddress?
    fun writeHttpResponse(response: HttpResponse, flush: Boolean = false, listener: ChannelFutureListener? = null)
    fun writeHttpContent(response: HttpContent, flush: Boolean = false, listener: ChannelFutureListener? = null)
}