package lighttunnel.ext

import io.netty.buffer.Unpooled
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.handler.codec.http.*
import lighttunnel.LightTunnelConfig
import lighttunnel.TunnelClient
import lighttunnel.conn.TunnelConnection
import lighttunnel.ext.httpserver.HttpServer
import lighttunnel.internal.base.util.basicAuthorization
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.*

fun TunnelClient.newHttpRpcServer(
    bossGroup: NioEventLoopGroup,
    workerGroup: NioEventLoopGroup,
    bindAddr: String?,
    bindPort: Int,
    authProvider: ((username: String, password: String) -> Boolean)? = null
): HttpServer {
    return HttpServer(
        bossGroup = bossGroup,
        workerGroup = workerGroup,
        bindAddr = bindAddr,
        bindPort = bindPort
    ) {
        intercept("^/.*".toRegex()) {
            val auth = authProvider ?: return@intercept null
            val account = it.basicAuthorization
            val next = if (account != null) auth(account.first, account.second) else false
            if (next) {
                null
            } else {
                val httpResponse = DefaultFullHttpResponse(it.protocolVersion(), HttpResponseStatus.UNAUTHORIZED)
                val content = HttpResponseStatus.UNAUTHORIZED.toString().toByteArray(StandardCharsets.UTF_8)
                httpResponse.headers().add(HttpHeaderNames.WWW_AUTHENTICATE, "Basic realm=.")
                httpResponse.headers().add(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
                httpResponse.headers().add(HttpHeaderNames.ACCEPT_RANGES, HttpHeaderValues.BYTES)
                httpResponse.headers().add(HttpHeaderNames.DATE, Date().toString())
                httpResponse.headers().add(HttpHeaderNames.CONTENT_LENGTH, content.size)
                httpResponse.content().writeBytes(content)
                httpResponse
            }
        }
        route("^/api/version".toRegex()) {
            val content = JSONObject().apply {
                put("name", "ltc")
                put("protoVersion", LightTunnelConfig.PROTO_VERSION)
                put("versionName", LightTunnelConfig.VERSION_NAME)
                put("versionCode", LightTunnelConfig.VERSION_CODE)
                put("buildDate", LightTunnelConfig.BUILD_DATA)
                put("commitSha", LightTunnelConfig.LAST_COMMIT_SHA)
                put("commitDate", LightTunnelConfig.LAST_COMMIT_DATE)
            }.let { Unpooled.copiedBuffer(it.toString(2), Charsets.UTF_8) }
            DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                content
            ).apply {
                headers()
                    .set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                    .set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes())
            }
        }
        route("^/api/snapshot".toRegex()) {
            val content = getTunnelConnectionList().tunnelConnectionListToJson().let {
                Unpooled.copiedBuffer(it.toString(2), Charsets.UTF_8)
            }
            DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                content
            ).also {
                it.headers()
                    .set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                    .set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes())
            }
        }
    }
}

private fun List<TunnelConnection>.tunnelConnectionListToJson(): JSONArray {
    return JSONArray(map {
        JSONObject().apply {
            put("name", it.tunnelRequest.name)
            put("request", it.toString())
            put("extras", it.tunnelRequest.extras)
        }
    })
}
