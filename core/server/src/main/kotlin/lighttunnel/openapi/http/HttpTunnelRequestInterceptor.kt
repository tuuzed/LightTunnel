package lighttunnel.openapi.http

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.HttpContent
import io.netty.handler.codec.http.HttpRequest
import lighttunnel.openapi.TunnelRequest

interface HttpTunnelRequestInterceptor {

    fun doHttpRequest(ctx: ChannelHandlerContext, httpRequest: HttpRequest, tunnelRequest: TunnelRequest): Boolean

    fun doHttpContent(ctx: ChannelHandlerContext, httpContent: HttpContent, tunnelRequest: TunnelRequest)

}
