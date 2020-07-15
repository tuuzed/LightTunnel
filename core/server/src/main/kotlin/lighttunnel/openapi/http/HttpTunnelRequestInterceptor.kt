package lighttunnel.openapi.http

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.FullHttpResponse
import lighttunnel.openapi.TunnelRequest

interface HttpTunnelRequestInterceptor {

    fun handleHttpRequest(ctx: ChannelHandlerContext, tunnelRequest: TunnelRequest, httpRequest: FullHttpRequest): FullHttpResponse?

    companion object {
        @JvmStatic
        val emptyImpl: HttpTunnelRequestInterceptor by lazy {
            object : HttpTunnelRequestInterceptor {
                override fun handleHttpRequest(ctx: ChannelHandlerContext, tunnelRequest: TunnelRequest, httpRequest: FullHttpRequest): FullHttpResponse? = null
            }
        }
    }

}
