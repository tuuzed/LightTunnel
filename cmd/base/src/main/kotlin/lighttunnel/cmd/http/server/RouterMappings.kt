package lighttunnel.cmd.http.server

import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.*
import io.netty.util.CharsetUtil
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class RouterMappings internal constructor() {

    private val mapping = ConcurrentHashMap<String, (request: FullHttpRequest) -> FullHttpResponse>()

    @Suppress("PrivatePropertyName")
    private val NOT_FOUND_ROUTE_CALLBACK: (request: FullHttpRequest) -> FullHttpResponse = {
        val content = Unpooled.copiedBuffer(HttpResponseStatus.NOT_FOUND.toString(), CharsetUtil.UTF_8)
        DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            HttpResponseStatus.NOT_FOUND,
            content
        ).also {
            it.headers()
                .set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
                .set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes())
        }
    }

    fun route(path: String, handler: (request: FullHttpRequest) -> FullHttpResponse) {
        mapping[path] = handler
    }

    @Throws(IOException::class)
    internal fun doHandle(request: FullHttpRequest): FullHttpResponse {
        val callback = mapping.getOrDefault(
            request.uri(), null)
            ?: return NOT_FOUND_ROUTE_CALLBACK(request)
        return callback(request)
    }

}