package lighttunnel.server.http

import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.HttpRequest

interface HttpPlugin {

    fun doHandle(request: HttpRequest): FullHttpResponse?

}