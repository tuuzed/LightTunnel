package lighttunnel.httpserver

import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.FullHttpResponse

fun interface RequestHandler {
    fun invoke(request: FullHttpRequest): FullHttpResponse
}