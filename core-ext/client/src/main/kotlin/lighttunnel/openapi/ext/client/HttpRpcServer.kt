package lighttunnel.openapi.ext.client

import io.netty.buffer.Unpooled
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.handler.codec.http.*
import lighttunnel.openapi.BuildConfig
import lighttunnel.openapi.TunnelClient
import lighttunnel.openapi.conn.TunnelConnection
import lighttunnel.openapi.ext.http.server.HttpServer
import lighttunnel.openapi.ext.name
import org.json.JSONArray
import org.json.JSONObject


fun newHttpRpcServer(
    bossGroup: NioEventLoopGroup,
    workerGroup: NioEventLoopGroup,
    bindAddr: String?,
    bindPort: Int,
    tunnelClient: TunnelClient
): HttpServer {
    return HttpServer(
        bossGroup = bossGroup,
        workerGroup = workerGroup,
        bindAddr = bindAddr,
        bindPort = bindPort
    ) {
        route("/api/version") {
            val content = JSONObject().apply {
                put("name", "ltc")
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
        route("/api/snapshot") {
            val content = tunnelClient.getTunnelConnectionList().tunnelConnectionListToJson().let {
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
            put("conn", it.toString())
        }
    })
}
