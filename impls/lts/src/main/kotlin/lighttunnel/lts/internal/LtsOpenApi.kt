package lighttunnel.lts.internal

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.handler.codec.http.*
import lighttunnel.common.extensions.JSONArrayOf
import lighttunnel.common.extensions.JSONObjectOf
import lighttunnel.common.extensions.basicAuthorization
import lighttunnel.common.utils.DateUtils
import lighttunnel.common.utils.ManifestUtils
import lighttunnel.httpserver.AuthProvider
import lighttunnel.httpserver.HttpServer
import java.util.*

internal class LtsOpenApi {

    fun start(
        bindIp: String?,
        bindPort: Int,
        authProvider: AuthProvider?
    ) {
        HttpServer(
            name = "LtsOpenApi",
            bossGroup = NioEventLoopGroup(),
            workerGroup = NioEventLoopGroup(),
            bindIp = bindIp,
            bindPort = bindPort,
        ) {
            intercept("^/.*".toRegex()) {
                val auth = authProvider ?: return@intercept null
                val account = it.basicAuthorization
                if (account != null) {
                    val (username, password) = account
                    if (auth.invoke(username, password)) {
                        return@intercept null
                    }
                }
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
            route("^/version".toRegex()) {
                Unpooled.copiedBuffer(
                    version.toString(2), Charsets.UTF_8
                ).newFullHttpResponse(HttpHeaderValues.APPLICATION_JSON)
            }
            route("^/snapshot".toRegex()) {
                Unpooled.copiedBuffer(
                    snapshot.toString(2), Charsets.UTF_8
                ).newFullHttpResponse(HttpHeaderValues.APPLICATION_JSON)
            }
        }.start()
    }

    private val version
        get() = JSONObjectOf(
            "appName" to ManifestUtils.appName,
            "version" to ManifestUtils.version,
            "buildDate" to ManifestUtils.buildDate,
            "commitHash" to ManifestUtils.commitHash,
            "commitDate" to ManifestUtils.commitDate,
        )

    private val snapshot
        get() = JSONObjectOf(
            "tcp" to JSONArrayOf(
                DataStore.tcp.map {
                    JSONObjectOf(
                        "localIp" to it.tunnelRequest.localIp,
                        "localPort" to it.tunnelRequest.localPort,
                        "remotePort" to it.tunnelRequest.remotePort,
                        "extras" to it.tunnelRequest.extras,
                        "conns" to it.connectionCount,
                        "inbound" to it.trafficStats.inboundBytes,
                        "outbound" to it.trafficStats.outboundBytes,
                        "createAt" to DateUtils.format(it.trafficStats.createAt),
                        "updateAt" to DateUtils.format(it.trafficStats.updateAt),
                    )
                }
            ),
            "http" to JSONArrayOf(
                DataStore.http.map {
                    JSONObjectOf(
                        "localIp" to it.tunnelRequest.localIp,
                        "localPort" to it.tunnelRequest.localPort,
                        "remotePort" to it.tunnelRequest.remotePort,
                        "extras" to it.tunnelRequest.extras,
                        "conns" to it.connectionCount,
                        "inbound" to it.trafficStats.inboundBytes,
                        "outbound" to it.trafficStats.outboundBytes,
                        "createAt" to DateUtils.format(it.trafficStats.createAt),
                        "updateAt" to DateUtils.format(it.trafficStats.updateAt),
                    )
                }
            ),
            "https" to JSONArrayOf(
                DataStore.https.map {
                    JSONObjectOf(
                        "localIp" to it.tunnelRequest.localIp,
                        "localPort" to it.tunnelRequest.localPort,
                        "remotePort" to it.tunnelRequest.remotePort,
                        "extras" to it.tunnelRequest.extras,
                        "conns" to it.connectionCount,
                        "inbound" to it.trafficStats.inboundBytes,
                        "outbound" to it.trafficStats.outboundBytes,
                        "createAt" to DateUtils.format(it.trafficStats.createAt),
                        "updateAt" to DateUtils.format(it.trafficStats.updateAt),
                    )
                }
            )
        )

    private fun ByteBuf.newFullHttpResponse(contentType: CharSequence): FullHttpResponse {
        return DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1, HttpResponseStatus.OK, this
        ).apply {
            headers()
                .set(HttpHeaderNames.CONTENT_TYPE, contentType)
                .set(HttpHeaderNames.CONTENT_LENGTH, this@newFullHttpResponse.readableBytes())
        }
    }
}
