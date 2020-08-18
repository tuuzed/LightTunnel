package lighttunnel.openapi.http

import io.netty.handler.codec.http.HttpContent
import io.netty.handler.codec.http.HttpRequest
import lighttunnel.openapi.TunnelRequest
import java.io.IOException

interface HttpTunnelRequestInterceptor {

    @Throws(IOException::class)
    fun doHttpRequest(ctx: HttpContext, httpRequest: HttpRequest, tunnelRequest: TunnelRequest): Boolean

    @Throws(IOException::class)
    fun doHttpContent(ctx: HttpContext, httpContent: HttpContent, tunnelRequest: TunnelRequest) {
    }

}
