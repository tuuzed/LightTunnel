package lighttunnel.openapi.http

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.HttpContent
import io.netty.handler.codec.http.HttpRequest

interface HttpPlugin {

    fun doHttpRequest(ctx: ChannelHandlerContext, httpRequest: HttpRequest): Boolean

    fun doHttpContent(ctx: ChannelHandlerContext, httpContent: HttpContent)

}