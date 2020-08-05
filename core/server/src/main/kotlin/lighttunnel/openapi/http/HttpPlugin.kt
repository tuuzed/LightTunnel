package lighttunnel.openapi.http

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.HttpContent
import io.netty.handler.codec.http.HttpRequest
import java.io.IOException

interface HttpPlugin {

    @Throws(IOException::class)
    fun doHttpRequest(ctx: ChannelHandlerContext, httpRequest: HttpRequest): Boolean

    @Throws(IOException::class)
    fun doHttpContent(ctx: ChannelHandlerContext, httpContent: HttpContent)

}