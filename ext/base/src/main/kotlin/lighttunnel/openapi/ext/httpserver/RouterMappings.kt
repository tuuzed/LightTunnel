package lighttunnel.openapi.ext.httpserver

import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.*
import io.netty.util.CharsetUtil
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class RouterMappings internal constructor() {

    private val handlers = ConcurrentHashMap<Regex, (request: FullHttpRequest) -> FullHttpResponse>()
    private val interceptors = ConcurrentHashMap<Regex, (request: FullHttpRequest) -> FullHttpResponse?>()

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

    fun intercept(path: Regex, interceptor: (request: FullHttpRequest) -> FullHttpResponse?) {
        interceptors[path] = interceptor
    }

    fun route(path: Regex, handler: (request: FullHttpRequest) -> FullHttpResponse) {
        handlers[path] = handler
    }

    @Throws(IOException::class)
    internal fun doHandle(request: FullHttpRequest): FullHttpResponse {
        val path = request.uri()
        val response = interceptors.keys()
            .toList()
            .firstOrNull { it.matches(path) }
            .let { interceptors[it] }
            ?.invoke(request)
        if (response != null) {
            return response
        }
        val handler = handlers.keys().toList().firstOrNull { it.matches(path) }.let { handlers[it] }
            ?: NOT_FOUND_ROUTE_CALLBACK
        return handler(request)
    }

}