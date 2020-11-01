package lighttunnel.ext

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.handler.codec.http.*
import lighttunnel.LightTunnelConfig
import lighttunnel.TunnelClient
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
                val content = HttpResponseStatus.UNAUTHORIZED.toString().toByteArray(StandardCharsets.UTF_8)
                DefaultFullHttpResponse(it.protocolVersion(), HttpResponseStatus.UNAUTHORIZED).apply {
                    headers().add(HttpHeaderNames.WWW_AUTHENTICATE, "Basic realm=.")
                    headers().add(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
                    headers().add(HttpHeaderNames.ACCEPT_RANGES, HttpHeaderValues.BYTES)
                    headers().add(HttpHeaderNames.DATE, Date().toString())
                    headers().add(HttpHeaderNames.CONTENT_LENGTH, content.size)
                    content().writeBytes(content)
                }
            }
        }
        route("^/api/version".toRegex()) {
            toVersionJson().let {
                Unpooled.copiedBuffer(it.toString(2), Charsets.UTF_8)
            }.newFullHttpResponse(HttpHeaderValues.APPLICATION_JSON)
        }
        route("^/api/snapshot".toRegex()) {
            toSnapshotJson().let {
                Unpooled.copiedBuffer(it.toString(2), Charsets.UTF_8)
            }.newFullHttpResponse(HttpHeaderValues.APPLICATION_JSON)
        }
    }
}

private fun ByteBuf.newFullHttpResponse(contentType: CharSequence) = DefaultFullHttpResponse(
    HttpVersion.HTTP_1_1,
    HttpResponseStatus.OK,
    this
).apply {
    headers()
        .set(HttpHeaderNames.CONTENT_TYPE, contentType)
        .set(HttpHeaderNames.CONTENT_LENGTH, this@newFullHttpResponse.readableBytes())
}

private fun TunnelClient.toSnapshotJson(): JSONArray {
    return JSONArray(getTunnelConnectionList().map {
        JSONObject().apply {
            put("name", it.tunnelRequest.name)
            put("request", it.toString())
            put("extras", it.tunnelRequest.extras)
        }
    })
}

private fun toVersionJson() = JSONObject().apply {
    put("name", "lts")
    put("protoVersion", LightTunnelConfig.PROTO_VERSION)
    put("versionName", LightTunnelConfig.VERSION_NAME)
    put("versionCode", LightTunnelConfig.VERSION_CODE)
    put("buildDate", LightTunnelConfig.BUILD_DATA)
    put("commitSha", LightTunnelConfig.LAST_COMMIT_SHA)
    put("commitDate", LightTunnelConfig.LAST_COMMIT_DATE)
}

