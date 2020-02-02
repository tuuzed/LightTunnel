package lighttunnel.dashboard.server

import io.netty.handler.codec.http.*
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class RouterConfig internal constructor() {
    private val mapping = ConcurrentHashMap<String, RouteCallback>()
    private var notfound: RouteCallback = {
        DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            HttpResponseStatus.NOT_FOUND
        )
    }

    fun route(path: String, callback: RouteCallback) {
        mapping[path] = callback
    }

    fun notfound(callback: RouteCallback) {
        this.notfound = callback
    }

    @Throws(IOException::class)
    internal fun doRequest(request: FullHttpRequest): FullHttpResponse {
        val callback = mapping.getOrDefault(request.uri(), null) ?: return notfound(request)
        return callback(request)
    }
}