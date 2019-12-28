package lighttunnel.api

import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.FullHttpResponse

interface ApiRequestCallback {

    fun doRequest(request: FullHttpRequest): FullHttpResponse

}