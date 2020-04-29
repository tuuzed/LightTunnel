package lighttunnel.api.server

import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.*
import io.netty.util.CharsetUtil
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class RouterConfig internal constructor() {
    private val mapping = ConcurrentHashMap<String, RouteCallback>()

    var notFoundRouteCallback: RouteCallback = {
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

    fun route(path: String, callback: RouteCallback) {
        mapping[path] = callback
    }

    @Throws(IOException::class)
    internal fun doRequest(request: FullHttpRequest): FullHttpResponse {
        val callback = mapping.getOrDefault(request.uri(), null) ?: return notFoundRouteCallback(request)
        return callback(request)
    }
}