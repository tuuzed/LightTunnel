package lighttunnel.server.http

import io.netty.handler.codec.http.HttpContent
import io.netty.handler.codec.http.HttpRequest
import java.io.IOException

interface HttpPlugin {

    @Throws(IOException::class)
    fun doHttpRequest(ctx: HttpContext, httpRequest: HttpRequest): Boolean

    @Throws(IOException::class)
    fun doHttpContent(ctx: HttpContext, httpContent: HttpContent) {
    }

}
