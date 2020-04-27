package lighttunnel.server.http

import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.*
import java.io.File
import java.net.URLDecoder

class HttpPluginImplStaticFile(
    private val rootPathList: List<String>,
    private val domainPrefixList: List<String>
) : HttpPlugin {

    override fun doHandle(request: FullHttpRequest): FullHttpResponse? {
        val host = request.headers().get(HttpHeaderNames.HOST)
        if (domainPrefixList.firstOrNull { it.startsWith(host) } == null) {
            return null
        }
        val path = URLDecoder.decode(request.uri().split('?').first(), "utf-8")
        val file = rootPathList.firstOrNull {
            File(it, path).let { f -> f.exists() && f.isFile }
        }?.let { File(it, path) }
        return if (file == null) {
            val content = "404 $path".toByteArray(Charsets.UTF_8)
            val response = DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.NOT_FOUND,
                Unpooled.wrappedBuffer(content)
            )
            response.headers()
                .set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
                .set(HttpHeaderNames.CONTENT_LENGTH, content.size)
            response
        } else {
            val content = file.readBytes()
            val response = DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.wrappedBuffer(content)
            )
            response.headers()
                .set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.BINARY)
                .set(HttpHeaderNames.CONTENT_LENGTH, content.size)
            response
        }
    }

}