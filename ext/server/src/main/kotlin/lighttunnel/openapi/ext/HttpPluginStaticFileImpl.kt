package lighttunnel.openapi.ext

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
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

    override fun doHttpRequest(ctx: ChannelHandlerContext, httpRequest: HttpRequest): Boolean {
        val host = HttpUtil.getHostWithoutPort(httpRequest)
        if (host == null || !hosts.contains(host)) {
            return false
        }
        val filename = URLDecoder.decode(httpRequest.uri().split('?').first(), "utf-8")
        val file = paths.map { File(it, filename) }.firstOrNull { it.exists() && it.isFile }
        if (file == null) {
            val content = Unpooled.copiedBuffer("404 $filename", CharsetUtil.UTF_8)
            ctx.write(DefaultHttpResponse(httpRequest.protocolVersion(), HttpResponseStatus.NOT_FOUND).apply {
                headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
                headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes())
            })
            ctx.write(DefaultHttpContent(Unpooled.wrappedBuffer(content)))
            ctx.writeAndFlush(DefaultLastHttpContent())
        } else {
            ctx.write(DefaultHttpResponse(httpRequest.protocolVersion(), HttpResponseStatus.OK).apply {
                headers().set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
                headers().set(HttpHeaderNames.CONTENT_LENGTH, file.inputStream().use { it.available() })
            })
            file.inputStream().use {
                val buf = ByteArray(4096)
                var length = it.read(buf)
                while (length != -1) {
                    ctx.write(DefaultHttpContent(Unpooled.copiedBuffer(buf, 0, length)))
                    length = it.read(buf)
                }
            }
            ctx.writeAndFlush(DefaultLastHttpContent())
        }
        return true
    }

    override fun doHttpContent(ctx: ChannelHandlerContext, httpContent: HttpContent) {}
}