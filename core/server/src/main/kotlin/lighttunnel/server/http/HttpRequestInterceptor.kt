package lighttunnel.server.http

import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse
import lighttunnel.proto.ProtoRequest
import java.net.SocketAddress

interface HttpRequestInterceptor {

    companion object {
        val EMPTY_IMPL = object : HttpRequestInterceptor {
            override fun handleHttpRequest(
                localAddress: SocketAddress,
                remoteAddress: SocketAddress,
                tpRequest: ProtoRequest,
                httpRequest: HttpRequest
            ): HttpResponse? = null
        }
    }

    fun handleHttpRequest(
        localAddress: SocketAddress,
        remoteAddress: SocketAddress,
        tpRequest: ProtoRequest,
        httpRequest: HttpRequest
    ): HttpResponse?

}
