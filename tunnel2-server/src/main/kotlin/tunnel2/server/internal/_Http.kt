@file:JvmName("_Http")

package tunnel2.server.internal


import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.handler.codec.base64.Base64
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse
import java.nio.charset.StandardCharsets

private const val CRLF = "\r\n"
private val CHARSET = StandardCharsets.UTF_8

fun HttpRequest.host(): String? {
    val host = this.headers().get(HttpHeaderNames.HOST)
    return host.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
}

fun HttpRequest.basicAuthorization(): Array<String>? {
    // Basic Z3Vlc3Q6Z3Vlc3Q=
    val authorizationValue = headers().get(HttpHeaderNames.AUTHORIZATION) ?: return null
    var tmp = authorizationValue.split(" ".toRegex()).dropLastWhile { it.isEmpty() }
    if (tmp.size != 2) return null
    val account = Base64.decode(
        Unpooled.wrappedBuffer(tmp[1].toByteArray(StandardCharsets.UTF_8))
    ).toString(CHARSET)
    tmp = account.split(":".toRegex()).dropLastWhile { it.isEmpty() }
    return if (tmp.size == 2) tmp.toTypedArray() else null
}

fun HttpRequest.toBytes(): ByteArray {
    val raw = StringBuilder()
    val method = this.method()
    val uri = this.uri()
    val httpVersion = this.protocolVersion()
    val headers = this.headers()
    raw.append(method.name()).append(" ").append(uri).append(" ").append(httpVersion.text()).append(CRLF)
    val iterator = headers.iteratorAsString()
    while (iterator.hasNext()) {
        val next = iterator.next()
        raw.append(next.key).append(": ").append(next.value).append(CRLF)
    }
    raw.append(CRLF)
    return raw.toString().toByteArray(CHARSET)
}

fun HttpResponse.toBytes(): ByteBuf {
    val raw = StringBuilder()
    val httpVersion = this.protocolVersion()
    val status = this.status()
    raw.append(httpVersion.text()).append(" ")
        .append(status.code()).append(" ").append(status.reasonPhrase())
        .append(CRLF)
    val headers = this.headers()
    val iterator = headers.iteratorAsString()
    while (iterator.hasNext()) {
        val next = iterator.next()
        raw.append(next.key).append(": ").append(next.value).append(CRLF)
    }
    raw.append(CRLF)
    return if (this is FullHttpResponse) {
        val responseLineAndHeader = raw.toString().toByteArray(CHARSET)
        val content = this.content()
        val buffer = Unpooled.buffer(responseLineAndHeader.size + content.readableBytes())
        buffer.writeBytes(responseLineAndHeader).writeBytes(content)
        buffer
    } else {
        Unpooled.wrappedBuffer(raw.toString().toByteArray(CHARSET))
    }
}

