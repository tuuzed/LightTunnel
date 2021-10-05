@file:JvmName("-HttpKt")

package lighttunnel.base.utils

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.handler.codec.base64.Base64
import io.netty.handler.codec.http.*
import java.nio.charset.StandardCharsets

private const val CRLF = "\r\n"
private val CHARSET = Charsets.UTF_8

val HttpRequest.hostExcludePort: String?
    get() {
        val host = this.headers().get(HttpHeaderNames.HOST) ?: return null
        return host.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
    }

val HttpRequest.basicAuthorization: Pair<String, String>?
    get() {
        // Basic Z3Vlc3Q6Z3Vlc3Q=
        val authorizationValue = this.headers().get(HttpHeaderNames.AUTHORIZATION) ?: return null
        var tmp = authorizationValue.split(" ".toRegex()).dropLastWhile { it.isEmpty() }
        if (tmp.size != 2) return null
        val account = Base64.decode(
            Unpooled.wrappedBuffer(tmp[1].toByteArray(StandardCharsets.UTF_8))
        ).toString(CHARSET)
        tmp = account.split(":".toRegex()).dropLastWhile { it.isEmpty() }
        return if (tmp.size == 2) tmp[0] to tmp[1] else null
    }

val HttpRequest.byteBuf: ByteBuf
    get() {
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
        return if (this is FullHttpRequest) {
            val responseLineAndHeader = raw.toString().toByteArray(CHARSET)
            val content = this.content()
            val buffer = Unpooled.buffer(responseLineAndHeader.size + content.readableBytes())
            buffer.writeBytes(responseLineAndHeader).writeBytes(content)
            buffer
        } else {
            Unpooled.wrappedBuffer(raw.toString().toByteArray(CHARSET))
        }
    }

val HttpResponse.byteBuf: ByteBuf
    get() {
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

val HttpContent.byteBuf: ByteBuf get() = this.content() ?: Unpooled.EMPTY_BUFFER
