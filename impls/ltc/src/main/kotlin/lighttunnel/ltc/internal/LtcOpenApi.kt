package lighttunnel.ltc.internal

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.handler.codec.http.*
import lighttunnel.client.Client
import lighttunnel.common.utils.ManifestUtils
import lighttunnel.common.utils.basicAuthorization
import lighttunnel.extras.name
import lighttunnel.httpserver.AuthProvider
import lighttunnel.httpserver.HttpServer
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

internal class LtcOpenApi(private val client: Client) {

    fun start(
        bindIp: String?,
        bindPort: Int,
        authProvider: AuthProvider?,
    ) {
        HttpServer(
            name = "LtcOpenApi",
            bossGroup = NioEventLoopGroup(),
            workerGroup = NioEventLoopGroup(),
            bindIp = bindIp,
            bindPort = bindPort,
        ) {
            intercept("^/.*".toRegex()) {
                val auth = authProvider ?: return@intercept null
                val account = it.basicAuthorization
                val next = if (account != null) auth.invoke(account.first, account.second) else false
                if (next) {
                    null
                } else {
                    val content = HttpResponseStatus.UNAUTHORIZED.toString().toByteArray()
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
            route("^/version".toRegex()) {
                Unpooled.copiedBuffer(version.toString(2), Charsets.UTF_8)
                    .newFullHttpResponse(HttpHeaderValues.APPLICATION_JSON)
            }
            route("^/snapshot".toRegex()) {
                Unpooled.copiedBuffer(snapshot.toString(2), Charsets.UTF_8)
                    .newFullHttpResponse(HttpHeaderValues.APPLICATION_JSON)
            }
        }.start()
    }

    private val version
        get() = JSONObject(linkedMapOf<String, Any>()).apply {
            put("appName", ManifestUtils.appName)
            put("version", ManifestUtils.version)
            put("buildDate", ManifestUtils.buildDate)
            put("commitHash", ManifestUtils.commitHash)
            put("commitDate", ManifestUtils.commitDate)
        }

    private val snapshot
        get() = JSONArray(client.getTunnelConnectionList().map {
            JSONObject().apply {
                put("name", it.tunnelRequest.name)
                put("request", it.toString())
                put("extras", it.tunnelRequest.extras)
            }
        })

    private fun ByteBuf.newFullHttpResponse(contentType: CharSequence) = DefaultFullHttpResponse(
        HttpVersion.HTTP_1_1, HttpResponseStatus.OK, this
    ).apply {
        headers().set(HttpHeaderNames.CONTENT_TYPE, contentType)
            .set(HttpHeaderNames.CONTENT_LENGTH, this@newFullHttpResponse.readableBytes())
    }
}
