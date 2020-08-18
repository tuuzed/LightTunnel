package lighttunnel.openapi.ext

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.handler.codec.http.*
import io.netty.util.CharsetUtil
import lighttunnel.base.util.hostExcludePort
import lighttunnel.openapi.http.HttpContext
import lighttunnel.openapi.http.HttpPlugin
import java.io.File
import java.net.URLDecoder

class HttpPluginStaticFileImpl(
    private val paths: List<String>,
    private val hosts: List<String>,
    private val bufferSize: Int = 4 * 1024
) : HttpPlugin {

    override fun doHttpRequest(ctx: HttpContext, httpRequest: HttpRequest): Boolean {
        val host = httpRequest.hostExcludePort
        if (host == null || !hosts.contains(host)) {
            return false
        }
        val filename = URLDecoder.decode(httpRequest.uri().split("?").first(), "utf-8")
        val file = paths.map { File(it, filename) }.firstOrNull { it.exists() && it.isFile }
        if (file == null) {
            val content = Unpooled.copiedBuffer("404/Not Found, $filename", CharsetUtil.UTF_8) ?: Unpooled.EMPTY_BUFFER
            ctx.write(DefaultHttpResponse(httpRequest.protocolVersion(), HttpResponseStatus.NOT_FOUND).apply {
                headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
                headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes())
            })
            ctx.write(DefaultHttpContent(Unpooled.wrappedBuffer(content)))
            ctx.write(LastHttpContent.EMPTY_LAST_CONTENT, flush = true, listener = ChannelFutureListener.CLOSE)
        } else {
            ctx.write(DefaultHttpResponse(httpRequest.protocolVersion(), HttpResponseStatus.OK).apply {
                headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
                headers().set(HttpHeaderNames.CONTENT_LENGTH, file.inputStream().use { it.available() })
            })
            file.inputStream().use {
                val buf = ByteArray(bufferSize)
                var length = it.read(buf)
                while (length != -1) {
                    ctx.write(DefaultHttpContent(Unpooled.copiedBuffer(buf, 0, length)))
                    length = it.read(buf)
                }
            }
            ctx.write(LastHttpContent.EMPTY_LAST_CONTENT, flush = true, listener = ChannelFutureListener.CLOSE)
        }
        return true
    }

}