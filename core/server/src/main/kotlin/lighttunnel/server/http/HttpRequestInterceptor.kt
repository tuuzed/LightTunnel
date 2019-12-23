package lighttunnel.server.http

import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse
import lighttunnel.proto.TunnelRequest
import java.net.SocketAddress

interface HttpRequestInterceptor {

    companion object {
        val emptyImpl by lazy {
            object : HttpRequestInterceptor {
                override fun handleHttpRequest(
                    localAddress: SocketAddress,
                    remoteAddress: SocketAddress,
                    tunnelRequest: TunnelRequest,
                    httpRequest: HttpRequest
                ): HttpResponse? = null
            }
        }
    }

    fun handleHttpRequest(
        localAddress: SocketAddress,
        remoteAddress: SocketAddress,
        tunnelRequest: TunnelRequest,
        httpRequest: HttpRequest
    ): HttpResponse?

}
