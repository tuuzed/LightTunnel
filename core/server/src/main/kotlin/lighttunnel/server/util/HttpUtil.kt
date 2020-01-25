package lighttunnel.server.util

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.handler.codec.base64.Base64
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse
import java.nio.charset.StandardCharsets


object HttpUtil {
    private const val CRLF = "\r\n"
    private val CHARSET = StandardCharsets.UTF_8

    fun getDomainHost(request: HttpRequest): String? {
        val host = request.headers().get(HttpHeaderNames.HOST) ?: return null
        return host.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
    }

    fun getBasicAuthorization(request: HttpRequest): Array<String>? {
        // Basic Z3Vlc3Q6Z3Vlc3Q=
        val authorizationValue = request.headers().get(HttpHeaderNames.AUTHORIZATION) ?: return null
        var tmp = authorizationValue.split(" ".toRegex()).dropLastWhile { it.isEmpty() }
        if (tmp.size != 2) return null
        val account = Base64.decode(
            Unpooled.wrappedBuffer(tmp[1].toByteArray(StandardCharsets.UTF_8))
        ).toString(CHARSET)
        tmp = account.split(":".toRegex()).dropLastWhile { it.isEmpty() }
        return if (tmp.size == 2) tmp.toTypedArray() else null
    }

    fun toBytes(request: HttpRequest): ByteArray {
        val raw = StringBuilder()
        val method = request.method()
        val uri = request.uri()
        val httpVersion = request.protocolVersion()
        val headers = request.headers()
        raw.append(method.name()).append(" ").append(uri).append(" ").append(httpVersion.text()).append(CRLF)
        val iterator = headers.iteratorAsString()
        while (iterator.hasNext()) {
            val next = iterator.next()
            raw.append(next.key).append(": ").append(next.value).append(CRLF)
        }
        raw.append(CRLF)
        return raw.toString().toByteArray(CHARSET)
    }

    fun toByteBuf(response: HttpResponse): ByteBuf {
        val raw = StringBuilder()
        val httpVersion = response.protocolVersion()
        val status = response.status()
        raw.append(httpVersion.text()).append(" ")
            .append(status.code()).append(" ").append(status.reasonPhrase())
            .append(CRLF)
        val headers = response.headers()
        val iterator = headers.iteratorAsString()
        while (iterator.hasNext()) {
            val next = iterator.next()
            raw.append(next.key).append(": ").append(next.value).append(CRLF)
        }
        raw.append(CRLF)
        return if (response is FullHttpResponse) {
            val responseLineAndHeader = raw.toString().toByteArray(CHARSET)
            val content = response.content()
            val buffer = Unpooled.buffer(responseLineAndHeader.size + content.readableBytes())
            buffer.writeBytes(responseLineAndHeader).writeBytes(content)
            buffer
        } else {
            Unpooled.wrappedBuffer(raw.toString().toByteArray(CHARSET))
        }
    }
}