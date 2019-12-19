package lighttunnel.server

import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse
import lighttunnel.proto.LTRequest
import java.net.SocketAddress

interface LTHttpRequestInterceptor {

    companion object {
        val EMPTY_IMPL = object : LTHttpRequestInterceptor {
            override fun handleHttpRequest(
                localAddress: SocketAddress,
                remoteAddress: SocketAddress,
                tpRequest: LTRequest,
                httpRequest: HttpRequest
            ): HttpResponse? = null
        }
    }

    fun handleHttpRequest(
        localAddress: SocketAddress,
        remoteAddress: SocketAddress,
        tpRequest: LTRequest,
        httpRequest: HttpRequest
    ): HttpResponse?

}
