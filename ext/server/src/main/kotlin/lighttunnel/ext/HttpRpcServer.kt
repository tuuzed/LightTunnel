@file:Suppress("DuplicatedCode")

package lighttunnel.ext

import com.jakewharton.picnic.table
import io.netty.buffer.ByteBuf
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
        route("^/view/snapshot".toRegex()) {
            toSnapshotTable().let {
                Unpooled.copiedBuffer(it.toString(), Charsets.UTF_8)
            }.newFullHttpResponse(HttpHeaderValues.TEXT_PLAIN)
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
    cellStyle {
        paddingLeft = 1
    }
    header {
        row(
            "#", "Name", "Type", "LocalNetwork", "RemotePort", "Host", "Conns", "Traffic-In/Out", "CreateAt", "UpdateAt"
        )
    }
    body {
        var index = 1
        for (fd in getTcpFdList()) {
            row(
                index++,
                fd.tunnelRequest.name?.let { it.substring(0, min(it.length, 16)) } ?: "-",
                "TCP",
                "${fd.tunnelRequest.localAddr}:${fd.tunnelRequest.localPort}",
                fd.tunnelRequest.remotePort,
                "-",
                fd.connectionCount,
                "${fd.statistics.inboundBytes}/${fd.statistics.outboundBytes}",
                fd.statistics.createAt.format(),
                fd.statistics.updateAt.format()
            )
        }
        for (fd in getHttpFdList()) {
            row(
                index++,
                fd.tunnelRequest.name?.let { it.substring(0, min(it.length, 16)) } ?: "-",
                "HTTP",
                "${fd.tunnelRequest.localAddr}:${fd.tunnelRequest.localPort}",
                "-",
                fd.tunnelRequest.host + ":" + httpPort,
                fd.connectionCount,
                "${fd.statistics.inboundBytes}/${fd.statistics.outboundBytes}",
                fd.statistics.createAt.format(),
                fd.statistics.updateAt.format()
            )
        }
        for (fd in getHttpsFdList()) {
            row(
                index++,
                fd.tunnelRequest.name?.let { it.substring(0, min(it.length, 16)) } ?: "-",
                "HTTPS",
                "${fd.tunnelRequest.localAddr}:${fd.tunnelRequest.localPort}",
                "-",
                fd.tunnelRequest.host + ":" + httpsPort,
                fd.connectionCount,
                "${fd.statistics.inboundBytes}/${fd.statistics.outboundBytes}",
                fd.statistics.createAt.format(),
                fd.statistics.updateAt.format()
            )
        }
    }
    footer {
        cellStyle {
            paddingTop = 1
        }
        row {
            cell("lts-V${LightTunnelConfig.VERSION_NAME}(${LightTunnelConfig.VERSION_CODE})") {
                columnSpan = 10
            }
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

