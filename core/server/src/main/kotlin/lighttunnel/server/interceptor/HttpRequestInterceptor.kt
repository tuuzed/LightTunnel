package lighttunnel.server.interceptor

import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse
import lighttunnel.proto.TunnelRequest
import java.net.SocketAddress

interface HttpRequestInterceptor {

    fun handleHttpRequest(
        localAddress: SocketAddress,
        remoteAddress: SocketAddress,
        tunnelRequest: TunnelRequest,
        httpRequest: HttpRequest
    ): HttpResponse? = null

}
