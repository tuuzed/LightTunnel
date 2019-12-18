package lighttunnel.server

import lighttunnel.proto.LTRequest
import io.netty.handler.codec.http.*
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.charset.StandardCharsets
import java.util.*

class LTHttpRequestInterceptorImpl : LTHttpRequestInterceptor {

    companion object {
        /** remote_addr 模板字符串 */
        private const val TL_REMOTE_ADDR = "\$remote_addr"
    }

    override fun handleHttpRequest(
        localAddress: SocketAddress, remoteAddress: SocketAddress,
        tpRequest: LTRequest, httpRequest: HttpRequest
    ): HttpResponse? {
        handleRewriteHttpHeaders(localAddress, remoteAddress, tpRequest, httpRequest)
        handleWriteHttpHeaders(localAddress, remoteAddress, tpRequest, httpRequest)
        return if (tpRequest.enableBasicAuth) handleHttpBasicAuth(tpRequest, httpRequest) else null
    }

    private fun handleHttpBasicAuth(tpRequest: LTRequest, httpRequest: HttpRequest): HttpResponse? {
        val account = httpRequest.basicAuthorization()
        val username = tpRequest.basicAuthUsername
        val password = tpRequest.basicAuthPassword
        if (account == null || username != account[0] || password != account[1]) {
            val httpResponse = DefaultFullHttpResponse(
                httpRequest.protocolVersion(),
                HttpResponseStatus.UNAUTHORIZED
            )
            val content = HttpResponseStatus.UNAUTHORIZED.toString().toByteArray(StandardCharsets.UTF_8)
            httpResponse.headers()
                .add(HttpHeaderNames.WWW_AUTHENTICATE, "Basic realm=\"${tpRequest.basicAuthRealm}\"")
            httpResponse.headers().add(HttpHeaderNames.CONNECTION, "keep-alive")
            httpResponse.headers().add(HttpHeaderNames.ACCEPT_RANGES, "bytes")

            httpResponse.headers().add(HttpHeaderNames.DATE, Date().toString())
            httpResponse.headers().add(HttpHeaderNames.CONTENT_LENGTH, content.size)
            httpResponse.content().writeBytes(content)
            return httpResponse
        }
        return null
    }

    private fun handleRewriteHttpHeaders(
        localAddress: SocketAddress, remoteAddress: SocketAddress,
        tpRequest: LTRequest, httpRequest: HttpRequest
    ) {
        handleProxyHttpHeaders(true, localAddress, remoteAddress, tpRequest, httpRequest)
    }

    private fun handleWriteHttpHeaders(
        localAddress: SocketAddress, remoteAddress: SocketAddress,
        tpRequest: LTRequest, httpRequest: HttpRequest
    ) {
        handleProxyHttpHeaders(false, localAddress, remoteAddress, tpRequest, httpRequest)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun handleProxyHttpHeaders(
        proxySet: Boolean,
        localAddress: SocketAddress, remoteAddress: SocketAddress,
        tpRequest: LTRequest, httpRequest: HttpRequest
    ) {
        val headers = if (proxySet) tpRequest.proxySetHeaders else tpRequest.proxyAddHeaders
        if (headers.isEmpty()) return
        for (it in headers.entries) {
            val name = it.key
            val value = it.value
            if (TL_REMOTE_ADDR == value) {
                if (remoteAddress is InetSocketAddress) {
                    val remoteAddr = remoteAddress.address.toString()
                    if (proxySet && httpRequest.headers().contains(name)) {
                        httpRequest.headers().set(name, remoteAddr)
                    } else {
                        httpRequest.headers().add(name, remoteAddr)
                    }
                }
            } else {
                if (proxySet && httpRequest.headers().contains(name)) {
                    httpRequest.headers().set(name, value)
                } else {
                    httpRequest.headers().add(name, value)
                }
            }
        }
    }


}
