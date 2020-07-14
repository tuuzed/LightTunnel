package lighttunnel.openapi.http

import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.FullHttpResponse
import lighttunnel.server.http.HttpPluginStaticFileImpl

interface HttpPlugin {

    fun doHandle(request: FullHttpRequest): FullHttpResponse?

    companion object {
        fun staticFileImpl(paths: List<String>, hosts: List<String>): HttpPlugin = HttpPluginStaticFileImpl(paths, hosts)
    }


}