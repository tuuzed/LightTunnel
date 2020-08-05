package lighttunnel.openapi.ext.httpserver

import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.*
import io.netty.util.CharsetUtil
import java.io.IOException
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class RouterMappings internal constructor() {

    private val lock = ReentrantReadWriteLock()
    private val interceptors = LinkedHashMap<Regex, (request: FullHttpRequest) -> FullHttpResponse?>()
    private val handlers = LinkedHashMap<Regex, (request: FullHttpRequest) -> FullHttpResponse>()

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

    fun intercept(path: Regex, interceptor: (request: FullHttpRequest) -> FullHttpResponse?) = lock.write { interceptors[path] = interceptor }

    fun route(path: Regex, handler: (request: FullHttpRequest) -> FullHttpResponse) = lock.write { handlers[path] = handler }

    @Throws(IOException::class)
    internal fun doHandle(request: FullHttpRequest): FullHttpResponse = lock.read {
        val path = request.uri().split("?").first()
        return interceptors.entries.firstOrNull { it.key.matches(path) }?.value?.invoke(request)
            ?: handlers.entries.firstOrNull { it.key.matches(path) }?.value?.invoke(request)
            ?: NOT_FOUND_ROUTE_CALLBACK.invoke(request)
    }

}