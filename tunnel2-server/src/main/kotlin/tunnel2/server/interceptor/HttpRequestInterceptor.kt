package tunnel2.server.interceptor

import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse
import tunnel2.common.TunnelRequest

import java.net.SocketAddress

interface HttpRequestInterceptor {

    companion object {
        val EMPTY_IMPL = object : HttpRequestInterceptor {}
    }

    fun handleHttpRequest(
        localAddress: SocketAddress,
        remoteAddress: SocketAddress,
        tunnelRequest: TunnelRequest,
        httpRequest: HttpRequest
    ): HttpResponse? = null

}
