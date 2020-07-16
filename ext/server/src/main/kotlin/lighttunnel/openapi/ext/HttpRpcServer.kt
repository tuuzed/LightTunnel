package lighttunnel.openapi.ext

import io.netty.buffer.Unpooled
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.handler.codec.http.*
import lighttunnel.openapi.BuildConfig
import lighttunnel.openapi.TunnelServer
import lighttunnel.openapi.ext.httpserver.HttpServer
import lighttunnel.openapi.ext.util.format
import lighttunnel.openapi.http.HttpFd
import lighttunnel.openapi.tcp.TcpFd
import org.json.JSONArray
import org.json.JSONObject

fun TunnelServer.newHttpRpcServer(
    bossGroup: NioEventLoopGroup,
    workerGroup: NioEventLoopGroup,
    bindAddr: String?,
    bindPort: Int
): HttpServer {
    return HttpServer(
        bossGroup = bossGroup,
        workerGroup = workerGroup,
        bindAddr = bindAddr,
        bindPort = bindPort
    ) {
        route("/api/version") {
            val content = JSONObject().apply {
                put("name", "lts")
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
        route("/api/snapshot") {
            val content = JSONObject().apply {
                put("tcp", getTcpFdList().tcpFdListToJson())
                put("http", getHttpFdList().httpFdListToJson())
                put("https", getHttpsFdList().httpFdListToJson())
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
    }
}


@Suppress("DuplicatedCode")
private fun List<TcpFd>.tcpFdListToJson(): JSONArray {
    return JSONArray(
        map { fd ->
            JSONObject().apply {
                put("port", fd.tunnelRequest.remotePort)
                put("conns", fd.connectionCount)
                put("name", fd.tunnelRequest.name)
                put("localAddr", fd.tunnelRequest.localAddr)
                put("localPort", fd.tunnelRequest.localPort)
                put("extras", fd.tunnelRequest.getExtras("ext.AUTH_TOKEN"))
                put("inboundBytes", fd.statistics.inboundBytes)
                put("outboundBytes", fd.statistics.outboundBytes)
                put("createAt", fd.statistics.createAt.format())
                put("updateAt", fd.statistics.updateAt.format())
            }
        }
    )
}

@Suppress("DuplicatedCode")
private fun List<HttpFd>.httpFdListToJson(): JSONArray {
    return JSONArray(
        map { fd ->
            JSONObject().apply {
                put("host", fd.tunnelRequest.host)
                put("conns", fd.connectionCount)
                put("name", fd.tunnelRequest.name)
                put("localAddr", fd.tunnelRequest.localAddr)
                put("localPort", fd.tunnelRequest.localPort)
                put("extras", fd.tunnelRequest.getExtras())
                put("inboundBytes", fd.statistics.inboundBytes)
                put("outboundBytes", fd.statistics.outboundBytes)
                put("createAt", fd.statistics.createAt.format())
                put("updateAt", fd.statistics.updateAt.format())
            }
        }
    )
}