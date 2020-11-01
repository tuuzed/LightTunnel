@file:Suppress("DuplicatedCode")

package lighttunnel.ext

import com.jakewharton.picnic.table
import io.netty.buffer.Unpooled
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.handler.codec.http.*
import lighttunnel.LightTunnelConfig
import lighttunnel.TunnelServer
import lighttunnel.ext.httpserver.HttpServer
import lighttunnel.http.HttpFd
import lighttunnel.internal.base.util.basicAuthorization
import lighttunnel.tcp.TcpFd
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.getOrSet
import kotlin.math.min

fun TunnelServer.newHttpRpcServer(
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
            val content = toVersionJson().let { Unpooled.copiedBuffer(it.toString(2), Charsets.UTF_8) }
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
            val content = toSnapshotJson().let { Unpooled.copiedBuffer(it.toString(2), Charsets.UTF_8) }
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
        route("^/view/snapshot".toRegex()) {
            val content = toSnapshotTable().let { Unpooled.copiedBuffer(it.toString(), Charsets.UTF_8) }
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
    }
}

private val t = ThreadLocal<MutableMap<String, DateFormat>>()

private fun getDateFormat(pattern: String) = t.getOrSet { hashMapOf() }[pattern]
    ?: SimpleDateFormat(pattern, Locale.getDefault()).also { t.get()[pattern] = it }

private fun Date?.format(pattern: String = "yyyy-MM-dd HH:mm:ss"): String? = this?.let { getDateFormat(pattern).format(this) }

private fun toVersionJson() = JSONObject().apply {
    put("name", "lts")
    put("protoVersion", LightTunnelConfig.PROTO_VERSION)
    put("versionName", LightTunnelConfig.VERSION_NAME)
    put("versionCode", LightTunnelConfig.VERSION_CODE)
    put("buildDate", LightTunnelConfig.BUILD_DATA)
    put("commitSha", LightTunnelConfig.LAST_COMMIT_SHA)
    put("commitDate", LightTunnelConfig.LAST_COMMIT_DATE)
}

private fun TunnelServer.toSnapshotJson() = JSONObject().apply {
    put("tcp", getTcpFdList().tcpFdListToJson())
    put("http", getHttpFdList().httpFdListToJson(httpPort))
    put("https", getHttpsFdList().httpFdListToJson(httpsPort))
}

private fun TunnelServer.toSnapshotTable() = table {
    header {
        cellStyle {
            paddingRight = 1
        }
        row(
            "Name", "Type", "LocalAddr", "LocalPort", "RemotePort", "Host", "Conns",
            "InboundBytes", "OutboundBytes", "CreateAt", "UpdateAt"
        )
    }
    body {
        cellStyle {
            paddingRight = 1
        }
        for (fd in getTcpFdList()) {
            row(
                try {
                    fd.tunnelRequest.extras.getString("ext.NAME")
                } catch (e: Exception) {
                    null
                }?.let { it.substring(0, min(it.length, 10)) } ?: "-",
                "TCP",
                fd.tunnelRequest.localAddr,
                fd.tunnelRequest.localPort,
                fd.tunnelRequest.remotePort,
                "-",
                fd.connectionCount,
                fd.statistics.inboundBytes,
                fd.statistics.outboundBytes,
                fd.statistics.createAt.format(),
                fd.statistics.updateAt.format()
            )
        }
        for (fd in getHttpFdList()) {
            row(
                try {
                    fd.tunnelRequest.extras.getString("ext.NAME")
                } catch (e: Exception) {
                    null
                }?.let { it.substring(0, min(it.length, 10)) } ?: "-",
                "HTTP",
                fd.tunnelRequest.localAddr,
                fd.tunnelRequest.localPort,
                "-",
                fd.tunnelRequest.host + ":" + httpPort,
                fd.connectionCount,
                fd.statistics.inboundBytes,
                fd.statistics.outboundBytes,
                fd.statistics.createAt.format(),
                fd.statistics.updateAt.format()
            )
        }
        for (fd in getHttpsFdList()) {
            row(
                try {
                    fd.tunnelRequest.extras.getString("ext.NAME")
                } catch (e: Exception) {
                    null
                }?.let { it.substring(0, min(it.length, 10)) } ?: "-",
                "HTTPS",
                fd.tunnelRequest.localAddr,
                fd.tunnelRequest.localPort,
                "-",
                fd.tunnelRequest.host + ":" + httpsPort,
                fd.connectionCount,
                fd.statistics.inboundBytes,
                fd.statistics.outboundBytes,
                fd.statistics.createAt.format(),
                fd.statistics.updateAt.format()
            )
        }
    }

}

private fun List<TcpFd>.tcpFdListToJson(): JSONArray {
    return JSONArray(
        map { fd ->
            JSONObject().apply {
                put("localAddr", fd.tunnelRequest.localAddr)
                put("localPort", fd.tunnelRequest.localPort)
                put("remotePort", fd.tunnelRequest.remotePort)
                put("extras", fd.tunnelRequest.extras)
                //
                put("conns", fd.connectionCount)
                put("inboundBytes", fd.statistics.inboundBytes)
                put("outboundBytes", fd.statistics.outboundBytes)
                put("createAt", fd.statistics.createAt.format())
                put("updateAt", fd.statistics.updateAt.format())
            }
        }
    )
}

private fun List<HttpFd>.httpFdListToJson(port: Int?): JSONArray {
    return JSONArray(
        map { fd ->
            JSONObject().apply {
                put("localAddr", fd.tunnelRequest.localAddr)
                put("localPort", fd.tunnelRequest.localPort)
                put("host", fd.tunnelRequest.host)
                put("port", port)
                put("extras", fd.tunnelRequest.extras)
                //
                put("conns", fd.connectionCount)
                put("inboundBytes", fd.statistics.inboundBytes)
                put("outboundBytes", fd.statistics.outboundBytes)
                put("createAt", fd.statistics.createAt.format())
                put("updateAt", fd.statistics.updateAt.format())
            }
        }
    )
}
