package lighttunnel.openapi.ext.server

import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.*
import io.netty.util.CharsetUtil
import lighttunnel.base.util.HttpUtil
import lighttunnel.openapi.http.HttpPlugin
import java.io.File
import java.net.URLDecoder

class HttpPluginStaticFileImpl(
    private val paths: List<String>,
    private val hosts: List<String>
) : HttpPlugin {
    override fun doHandle(request: FullHttpRequest): FullHttpResponse? {
        val host = HttpUtil.getHostWithoutPort(request)
        if (host == null || !hosts.contains(host)) {
            return null
        }
        val filename = URLDecoder.decode(request.uri().split('?').first(), "utf-8")
        val file = paths.map { File(it, filename) }.firstOrNull { it.exists() && it.isFile }
        return if (file == null) {
            val content = Unpooled.copiedBuffer("404 $filename", CharsetUtil.UTF_8)
            DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.NOT_FOUND,
                content
            ).apply {
                headers()
                    .set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
                    .set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes())
            }

        } else {
            val content = file.readBytes()
            DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.wrappedBuffer(content)
            ).apply {
                headers()
                    .set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.BINARY)
                    .set(HttpHeaderNames.CONTENT_LENGTH, content.size)
            }
        }
    }
}