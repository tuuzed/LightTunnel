package lighttunnel.server.http

import io.netty.handler.codec.http.HttpContent
import io.netty.handler.codec.http.HttpRequest
import lighttunnel.common.entity.TunnelRequest
import java.io.IOException

interface HttpTunnelRequestInterceptor {

    @Throws(IOException::class)
    fun doHttpRequest(ctx: HttpContext, httpRequest: HttpRequest, tunnelRequest: TunnelRequest): Boolean = false

    @Throws(IOException::class)
    fun doHttpContent(ctx: HttpContext, httpContent: HttpContent, tunnelRequest: TunnelRequest) = Unit

}
