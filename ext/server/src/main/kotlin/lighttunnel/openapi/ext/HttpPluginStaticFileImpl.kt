package lighttunnel.openapi.ext

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.handler.codec.http.*
import io.netty.util.CharsetUtil
import lighttunnel.base.util.hostExcludePort
import lighttunnel.openapi.http.HttpChain
import lighttunnel.openapi.http.HttpPlugin
import java.io.File
import java.net.URLDecoder

class HttpPluginStaticFileImpl(
    private val paths: List<String>,
    private val hosts: List<String>
) : HttpPlugin {

    override fun doHttpRequest(chain: HttpChain, httpRequest: HttpRequest): Boolean {
        val host = httpRequest.hostExcludePort
        if (host == null || !hosts.contains(host)) {
            return false
        }
        val filename = URLDecoder.decode(httpRequest.uri().split('?').first(), "utf-8")
        val file = paths.map { File(it, filename) }.firstOrNull { it.exists() && it.isFile }
        if (file == null) {
            val content = Unpooled.copiedBuffer("404 $filename", CharsetUtil.UTF_8)
            chain.writeHttpResponse(DefaultHttpResponse(httpRequest.protocolVersion(), HttpResponseStatus.NOT_FOUND).apply {
                headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
                headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes())
            })
            chain.writeHttpContent(DefaultHttpContent(Unpooled.wrappedBuffer(content)))
            chain.writeHttpContent(DefaultLastHttpContent())
        } else {
            chain.writeHttpResponse(DefaultHttpResponse(httpRequest.protocolVersion(), HttpResponseStatus.OK).apply {
                headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
                headers().set(HttpHeaderNames.CONTENT_LENGTH, file.inputStream().use { it.available() })
            })
            file.inputStream().use {
                val buf = ByteArray(4096)
                var length = it.read(buf)
                while (length != -1) {
                    chain.writeHttpContent(DefaultHttpContent(Unpooled.copiedBuffer(buf, 0, length)))
                    length = it.read(buf)
                }
            }
            chain.writeHttpContent(LastHttpContent.EMPTY_LAST_CONTENT, flush = true, listener = ChannelFutureListener.CLOSE)
        }
        return true
    }

    override fun doHttpContent(chain: HttpChain, httpContent: HttpContent) {}
}