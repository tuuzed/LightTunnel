package lighttunnel.openapi.http

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.FullHttpResponse
import lighttunnel.openapi.TunnelRequest
import lighttunnel.server.http.HttpRequestInterceptorDefaultImpl

interface HttpRequestInterceptor {

    fun handleHttpRequest(ctx: ChannelHandlerContext, tunnelRequest: TunnelRequest, httpRequest: FullHttpRequest): FullHttpResponse?

    companion object {
        val defaultImpl: HttpRequestInterceptor by lazy { HttpRequestInterceptorDefaultImpl() }
    }

}
