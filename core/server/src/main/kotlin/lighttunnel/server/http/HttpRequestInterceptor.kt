package lighttunnel.server.http

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
import lighttunnel.proto.TunnelRequest
import lighttunnel.util.HttpUtil
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.charset.StandardCharsets
import java.util.*

interface HttpRequestInterceptor {

    fun handleHttpRequest(ctx: ChannelHandlerContext, tunnelRequest: TunnelRequest, httpRequest: FullHttpRequest): FullHttpResponse? = null

    companion object {
        @JvmStatic
        val defaultImpl: HttpRequestInterceptor by lazy { DefaultImpl() }
    }

    private class DefaultImpl : HttpRequestInterceptor {

        companion object {
            /** remote_addr 魔法值 */
            private const val MAGIC_VALUE_REMOTE_ADDR = "\$remote_addr"
        }

        override fun handleHttpRequest(ctx: ChannelHandlerContext, tunnelRequest: TunnelRequest, httpRequest: FullHttpRequest): FullHttpResponse? {
            val localAddress = ctx.channel().localAddress()
            val remoteAddress = ctx.channel().remoteAddress()
            handleRewriteHttpHeaders(localAddress, remoteAddress, tunnelRequest, httpRequest)
            handleWriteHttpHeaders(localAddress, remoteAddress, tunnelRequest, httpRequest)
            return if (tunnelRequest.enableBasicAuth) handleHttpBasicAuth(tunnelRequest, httpRequest) else null
        }

        private fun handleHttpBasicAuth(tunnelRequest: TunnelRequest, httpRequest: FullHttpRequest): FullHttpResponse? {
            val account = HttpUtil.getBasicAuthorization(httpRequest)
            val username = tunnelRequest.basicAuthUsername
            val password = tunnelRequest.basicAuthPassword
            if (account == null || username != account[0] || password != account[1]) {
                val httpResponse = DefaultFullHttpResponse(
                    httpRequest.protocolVersion(),
                    HttpResponseStatus.UNAUTHORIZED
                )
                val content = HttpResponseStatus.UNAUTHORIZED.toString().toByteArray(StandardCharsets.UTF_8)
                httpResponse.headers().add(
                    HttpHeaderNames.WWW_AUTHENTICATE, "Basic realm=\"${tunnelRequest.basicAuthRealm}\""
                )
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
            tunnelRequest: TunnelRequest, httpRequest: FullHttpRequest
        ) = handleProxyHttpHeaders(true, localAddress, remoteAddress, tunnelRequest, httpRequest)

        private fun handleWriteHttpHeaders(
            localAddress: SocketAddress, remoteAddress: SocketAddress,
            tunnelRequest: TunnelRequest, httpRequest: FullHttpRequest
        ) = handleProxyHttpHeaders(false, localAddress, remoteAddress, tunnelRequest, httpRequest)

        @Suppress("UNUSED_PARAMETER")
        private fun handleProxyHttpHeaders(
            pxySet: Boolean,
            localAddress: SocketAddress, remoteAddress: SocketAddress,
            tunnelRequest: TunnelRequest, httpRequest: FullHttpRequest
        ) {
            val headers = if (pxySet) tunnelRequest.pxySetHeaders else tunnelRequest.pxyAddHeaders
            if (headers.isEmpty()) {
                return
            }
            val remoteAddr = if (remoteAddress is InetSocketAddress) remoteAddress.address.toString() else null
            for (it in headers.entries) {
                val name = it.key
                val value = when (it.value) {
                    MAGIC_VALUE_REMOTE_ADDR -> remoteAddr
                    else -> it.value
                } ?: continue
                if (pxySet && httpRequest.headers().contains(name)) {
                    httpRequest.headers().set(name, value)
                } else {
                    httpRequest.headers().add(name, value)
                }
            }
        }
    }

}
