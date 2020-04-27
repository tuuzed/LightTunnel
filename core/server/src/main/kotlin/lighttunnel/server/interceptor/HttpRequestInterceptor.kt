package lighttunnel.server.interceptor

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.FullHttpResponse
import lighttunnel.proto.TunnelRequest

interface HttpRequestInterceptor {

    fun handleHttpRequest(ctx: ChannelHandlerContext, tunnelRequest: TunnelRequest, httpRequest: FullHttpRequest): FullHttpResponse? = null

}
