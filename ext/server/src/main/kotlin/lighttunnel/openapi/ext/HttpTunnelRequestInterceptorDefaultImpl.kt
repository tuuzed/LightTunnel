package lighttunnel.openapi.ext

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
import lighttunnel.base.util.HttpUtil
import lighttunnel.openapi.TunnelRequest
import lighttunnel.openapi.http.HttpTunnelRequestInterceptor
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.charset.StandardCharsets
import java.util.*

class HttpTunnelRequestInterceptorDefaultImpl : HttpTunnelRequestInterceptor {

    companion object {
        /** remote_addr 魔法值 */
        private const val MAGIC_VALUE_REMOTE_ADDR = "\$remote_addr"
    }


    override fun doHttpRequest(ctx: ChannelHandlerContext, httpRequest: HttpRequest, tunnelRequest: TunnelRequest): Boolean {
        val localAddress = ctx.channel().localAddress()
        val remoteAddress = ctx.channel().remoteAddress()
        handleRewriteHttpHeaders(localAddress, remoteAddress, tunnelRequest, httpRequest)
        handleWriteHttpHeaders(localAddress, remoteAddress, tunnelRequest, httpRequest)
        return tunnelRequest.enableBasicAuth && handleHttpBasicAuth(ctx, tunnelRequest, httpRequest)
    }

    override fun doHttpContent(ctx: ChannelHandlerContext, httpContent: HttpContent, tunnelRequest: TunnelRequest) {}


    private fun handleHttpBasicAuth(ctx: ChannelHandlerContext, tunnelRequest: TunnelRequest, httpRequest: HttpRequest): Boolean {
        val account = HttpUtil.getBasicAuthorization(httpRequest)
        val username = tunnelRequest.basicAuthUsername
        val password = tunnelRequest.basicAuthPassword
        if (account?.size != 2 || username != account[0] || password != account[1]) {
            val content = HttpResponseStatus.UNAUTHORIZED.toString().toByteArray(StandardCharsets.UTF_8)
            ctx.write(DefaultHttpResponse(httpRequest.protocolVersion(), HttpResponseStatus.UNAUTHORIZED).apply {
                headers().add(HttpHeaderNames.WWW_AUTHENTICATE, "Basic realm=\"${tunnelRequest.basicAuthRealm}\"")
                headers().add(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
                headers().add(HttpHeaderNames.ACCEPT_RANGES, HttpHeaderValues.BYTES)
                headers().add(HttpHeaderNames.DATE, Date().toString())
                headers().add(HttpHeaderNames.CONTENT_LENGTH, content.size)
            })
            ctx.write(DefaultHttpContent(Unpooled.wrappedBuffer(content)))
            ctx.writeAndFlush(DefaultLastHttpContent())
            return true
        }
        return false
    }

    private fun handleRewriteHttpHeaders(
        localAddress: SocketAddress,
        remoteAddress: SocketAddress,
        tunnelRequest: TunnelRequest,
        httpRequest: HttpRequest
    ) = handleProxyHttpHeaders(
        tunnelRequest.pxyAddHeaders,
        localAddress,
        remoteAddress,
        tunnelRequest,
        httpRequest
    ) { name, value -> add(name, value) }

    private fun handleWriteHttpHeaders(
        localAddress: SocketAddress,
        remoteAddress: SocketAddress,
        tunnelRequest: TunnelRequest,
        httpRequest: HttpRequest
    ) = handleProxyHttpHeaders(
        tunnelRequest.pxySetHeaders,
        localAddress,
        remoteAddress,
        tunnelRequest,
        httpRequest
    ) { name, value -> if (contains(name)) set(name, value) }

    @Suppress("UNUSED_PARAMETER")
    private inline fun handleProxyHttpHeaders(
        pxyHeaders: Map<String, String>,
        localAddress: SocketAddress,
        remoteAddress: SocketAddress,
        tunnelRequest: TunnelRequest,
        httpRequest: HttpRequest,
        apply: HttpHeaders.(name: String, value: String) -> Unit
    ) {
        if (pxyHeaders.isEmpty()) {
            return
        }
        for (it in pxyHeaders.entries) {
            val name = it.key
            // 需要处理魔法值
            val value = when (it.value) {
                MAGIC_VALUE_REMOTE_ADDR -> if (remoteAddress is InetSocketAddress) remoteAddress.address.toString() else null
                else -> it.value
            } ?: continue
            httpRequest.headers().apply(name, value)
        }
    }


}