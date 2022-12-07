package lighttunnel.httpclient

import io.netty.handler.codec.http.FullHttpResponse

fun interface RequestCallback {
    fun invoke(response: FullHttpResponse)
}