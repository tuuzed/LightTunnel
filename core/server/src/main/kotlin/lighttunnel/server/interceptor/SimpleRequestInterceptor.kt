package lighttunnel.server.interceptor

import io.netty.handler.codec.http.*
import lighttunnel.proto.ProtoException
import lighttunnel.proto.TunnelRequest
import lighttunnel.proto.TunnelRequest.Factory.copyTcp
import lighttunnel.util.PortUtil
import lighttunnel.util.http.basicAuthorization
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.charset.StandardCharsets
import java.util.*

class SimpleRequestInterceptor(
    /** 预置Token */
    private val authToken: String? = null,
    /** 端口白名单 */
    private val allowPorts: String? = null
) : TunnelRequestInterceptor, HttpRequestInterceptor {

    companion object {
        /** remote_addr 模板字符串 */
        private const val TL_REMOTE_ADDR = "\$remote_addr"

        val defaultImpl by lazy { SimpleRequestInterceptor() }
    }


    @Throws(ProtoException::class)
    override fun handleTunnelRequest(tunnelRequest: TunnelRequest): TunnelRequest {
        if (authToken != null && authToken != tunnelRequest.authToken) {
            throw ProtoException("request($tunnelRequest), Bad Auth Token(${tunnelRequest.authToken})")
        }
        return when (tunnelRequest.type) {
            TunnelRequest.Type.TCP -> {
                if (tunnelRequest.remotePort == 0) {
                    tunnelRequest.copyTcp(
                        remotePort = PortUtil.getAvailableTcpPort(allowPorts ?: "1024-65535")
                    )
                } else {
                    if (allowPorts != null && !PortUtil.hasInPortRange(allowPorts, tunnelRequest.remotePort)) {
                        throw ProtoException("request($tunnelRequest), remotePort($tunnelRequest.remotePort) Not allowed to use.")
                    }
                    tunnelRequest
                }
            }
            else -> {
                tunnelRequest
            }
        }
    }

    override fun handleHttpRequest(
        localAddress: SocketAddress, remoteAddress: SocketAddress,
        tunnelRequest: TunnelRequest, httpRequest: HttpRequest
    ): HttpResponse? {
        handleRewriteHttpHeaders(localAddress, remoteAddress, tunnelRequest, httpRequest)
        handleWriteHttpHeaders(localAddress, remoteAddress, tunnelRequest, httpRequest)
        return if (tunnelRequest.enableBasicAuth) handleHttpBasicAuth(tunnelRequest, httpRequest) else null
    }

    private fun handleHttpBasicAuth(
        tunnelRequest: TunnelRequest, httpRequest: HttpRequest
    ): HttpResponse? {
        val account = httpRequest.basicAuthorization
        val username = tunnelRequest.basicAuthUsername
        val password = tunnelRequest.basicAuthPassword
        if (account == null || username != account[0] || password != account[1]) {
            val httpResponse = DefaultFullHttpResponse(
                httpRequest.protocolVersion(),
                HttpResponseStatus.UNAUTHORIZED
            )
            val content = HttpResponseStatus.UNAUTHORIZED.toString().toByteArray(StandardCharsets.UTF_8)
            httpResponse.headers()
                .add(HttpHeaderNames.WWW_AUTHENTICATE, "Basic realm=\"${tunnelRequest.basicAuthRealm}\"")
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
        tunnelRequest: TunnelRequest, httpRequest: HttpRequest
    ) {
        handleProxyHttpHeaders(true, localAddress, remoteAddress, tunnelRequest, httpRequest)
    }

    private fun handleWriteHttpHeaders(
        localAddress: SocketAddress, remoteAddress: SocketAddress,
        tunnelRequest: TunnelRequest, httpRequest: HttpRequest
    ) {
        handleProxyHttpHeaders(false, localAddress, remoteAddress, tunnelRequest, httpRequest)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun handleProxyHttpHeaders(
        isPxySet: Boolean,
        localAddress: SocketAddress, remoteAddress: SocketAddress,
        tunnelRequest: TunnelRequest, httpRequest: HttpRequest
    ) {
        val headers = if (isPxySet) tunnelRequest.pxySetHeaders else tunnelRequest.pxyAddHeaders
        if (headers.isEmpty()) {
            return
        }
        for (it in headers.entries) {
            val name = it.key
            val value = it.value
            if (TL_REMOTE_ADDR == value) {
                if (remoteAddress is InetSocketAddress) {
                    val remoteAddr = remoteAddress.address.toString()
                    if (isPxySet && httpRequest.headers().contains(name)) {
                        httpRequest.headers().set(name, remoteAddr)
                    } else {
                        httpRequest.headers().add(name, remoteAddr)
                    }
                }
            } else {
                if (isPxySet && httpRequest.headers().contains(name)) {
                    httpRequest.headers().set(name, value)
                } else {
                    httpRequest.headers().add(name, value)
                }
            }
        }
    }
}
