package tunnel2.server.interceptor


import io.netty.handler.codec.http.*
import tunnel2.common.TunnelException
import tunnel2.common.TunnelRequest
import tunnel2.common.TunnelType
import tunnel2.server.internal.basicAuthorization
import tunnel2.server.internal.hasInPortRange
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.charset.StandardCharsets
import java.util.*

class SimpleRequestInterceptor(
    /** 预置Token */
    private val presetAuthToken: String,
    /** 端口白名单 */
    private val allowPorts: String?
) : TunnelRequestInterceptor, HttpRequestInterceptor {

    companion object {
        /** remote_addr 模板字符串 */
        private const val TL_REMOTE_ADDR = "\$remote_addr"
    }

    @Throws(TunnelException::class)
    override fun handleTunnelRequest(request: TunnelRequest): TunnelRequest {
        verifyToken(request)
        return when (request.type) {
            TunnelType.TCP -> {
                val remotePort = request.remotePort
                if (allowPorts != null && !hasInPortRange(allowPorts, remotePort)) {
                    throw TunnelException("request($request), remotePort($remotePort) Not allowed to use.")
                }
                request
            }
            else -> request
        }
    }

    /**
     * 验证Token
     */
    @Throws(TunnelException::class)
    private fun verifyToken(request: TunnelRequest) {
        if (presetAuthToken != request.authToken) {
            throw TunnelException("request($request), Bad Auth Token(${request.authToken})")
        }
    }

    override fun handleHttpRequest(
        localAddress: SocketAddress,
        remoteAddress: SocketAddress,
        tunnelRequest: TunnelRequest,
        httpRequest: HttpRequest
    ): HttpResponse? {
        handleRewriteHttpHeaders(localAddress, remoteAddress, tunnelRequest, httpRequest)
        handleWriteHttpHeaders(localAddress, remoteAddress, tunnelRequest, httpRequest)
        return if (tunnelRequest.enableBasicAuth) handleHttpBasicAuth(tunnelRequest, httpRequest) else null
    }

    private fun handleHttpBasicAuth(tunnelRequest: TunnelRequest, httpRequest: HttpRequest): HttpResponse? {
        val account = httpRequest.basicAuthorization()
        val username = tunnelRequest.basicAuthUsername
        val password = tunnelRequest.basicAuthPassword
        if (account == null || username != account[0] || password != account[1]) {
            val httpResponse = DefaultFullHttpResponse(
                httpRequest.protocolVersion(),
                HttpResponseStatus.UNAUTHORIZED
            )
            val content = HttpResponseStatus.UNAUTHORIZED.toString().toByteArray(StandardCharsets.UTF_8)
            httpResponse.headers().add(HttpHeaderNames.WWW_AUTHENTICATE, "Basic realm=\"${tunnelRequest.basicAuthRealm}\"")
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
        localAddress: SocketAddress,
        remoteAddress: SocketAddress,
        tunnelRequest: TunnelRequest,
        httpRequest: HttpRequest
    ) {
        handleProxyHttpHeaders(true, localAddress, remoteAddress, tunnelRequest, httpRequest)
    }

    private fun handleWriteHttpHeaders(
        localAddress: SocketAddress,
        remoteAddress: SocketAddress,
        tunnelRequest: TunnelRequest,
        httpRequest: HttpRequest
    ) {
        handleProxyHttpHeaders(false, localAddress, remoteAddress, tunnelRequest, httpRequest)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun handleProxyHttpHeaders(
        proxySet: Boolean,
        localAddress: SocketAddress,
        remoteAddress: SocketAddress,
        tunnelRequest: TunnelRequest,
        httpRequest: HttpRequest
    ) {

        val headers = if (proxySet) tunnelRequest.proxySetHeaders else tunnelRequest.proxyAddHeaders
        if (headers.isEmpty()) {
            return
        }
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
