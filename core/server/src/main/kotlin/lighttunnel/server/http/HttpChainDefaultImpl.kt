package lighttunnel.server.http

import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.HttpContent
import io.netty.handler.codec.http.HttpResponse
import lighttunnel.base.util.byteBuf
import lighttunnel.openapi.http.HttpChain
import java.net.SocketAddress

internal class HttpChainDefaultImpl(
    private val ctx: ChannelHandlerContext
) : HttpChain {

    override val localAddress: SocketAddress? = ctx.channel().localAddress()

    override val remoteAddress: SocketAddress? = ctx.channel().remoteAddress()

    override fun writeHttpResponse(response: HttpResponse, flush: Boolean, listener: ChannelFutureListener?) {
        val channelFuture = if (flush) {
            ctx.write(response.byteBuf)
        } else {
            ctx.writeAndFlush(response.byteBuf)
        }
        if (listener != null) {
            channelFuture.addListener(listener)
        }
    }

    override fun writeHttpContent(response: HttpContent, flush: Boolean, listener: ChannelFutureListener?) {
        val channelFuture = if (flush) {
            ctx.write(response.byteBuf)
        } else {
            ctx.writeAndFlush(response.byteBuf)
        }
        if (listener != null) {
            channelFuture.addListener(listener)
        }
    }

}