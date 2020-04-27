package lighttunnel.server.http

import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.FullHttpResponse

interface HttpPlugin {

    fun doHandle(request: FullHttpRequest): FullHttpResponse?

}