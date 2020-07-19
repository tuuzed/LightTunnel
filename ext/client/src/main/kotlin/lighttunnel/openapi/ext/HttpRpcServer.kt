package lighttunnel.openapi.ext

import io.netty.buffer.Unpooled
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.handler.codec.http.*
import lighttunnel.base.util.HttpUtil
import lighttunnel.openapi.BuildConfig
import lighttunnel.openapi.TunnelClient
import lighttunnel.openapi.conn.TunnelConnection
import lighttunnel.openapi.ext.httpserver.HttpServer
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
            val account = HttpUtil.getBasicAuthorization(it)
            val next = if (account?.size == 2) auth(account[0], account[1]) else false
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
                put("protoVersion", BuildConfig.PROTO_VERSION)
                put("versionName", BuildConfig.VERSION_NAME)
                put("versionCode", BuildConfig.VERSION_CODE)
                put("buildDate", BuildConfig.BUILD_DATA)
                put("commitSha", BuildConfig.LAST_COMMIT_SHA)
                put("commitDate", BuildConfig.LAST_COMMIT_DATE)
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
