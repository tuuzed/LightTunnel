package tpserver

import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse
import tpcommon.TPRequest

import java.net.SocketAddress

interface TPHttpRequestInterceptor {

    companion object {
        val EMPTY_IMPL = object : TPHttpRequestInterceptor {
            override fun handleHttpRequest(
                localAddress: SocketAddress,
                remoteAddress: SocketAddress,
                tpRequest: TPRequest,
                httpRequest: HttpRequest
            ): HttpResponse? = null
        }
    }

    fun handleHttpRequest(
        localAddress: SocketAddress,
        remoteAddress: SocketAddress,
        tpRequest: TPRequest,
        httpRequest: HttpRequest
    ): HttpResponse?

}
