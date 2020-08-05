package lighttunnel.openapi.http

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.HttpContent
import io.netty.handler.codec.http.HttpRequest
import lighttunnel.openapi.TunnelRequest
import java.io.IOException

interface HttpTunnelRequestInterceptor {

    @Throws(IOException::class)
    fun doHttpRequest(ctx: ChannelHandlerContext, httpRequest: HttpRequest, tunnelRequest: TunnelRequest): Boolean

    @Throws(IOException::class)
    fun doHttpContent(ctx: ChannelHandlerContext, httpContent: HttpContent, tunnelRequest: TunnelRequest)

}
