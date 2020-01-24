package lighttunnel.server

import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.*
import lighttunnel.api.ApiServer
import lighttunnel.server.http.HttpRegistry
import lighttunnel.server.tcp.TcpRegistry
import org.json.JSONArray
import org.json.JSONObject

class DashRequestDispatcher(
    private val tcpRegistry: TcpRegistry?,
    private val httpRegistry: HttpRegistry?,
    private val httpsRegistry: HttpRegistry?
) : ApiServer.RequestDispatcher {

    companion object {
        private val EMPTY_JSON_ARRAY = JSONArray()
    }

    override fun doRequest(request: FullHttpRequest): FullHttpResponse {
        return when {
            "/api/snapshot" == request.uri() -> {
                val obj = JSONObject()
                obj.put("tcp", tcpRegistry?.snapshot ?: EMPTY_JSON_ARRAY)
                obj.put("http", httpRegistry?.snapshot ?: EMPTY_JSON_ARRAY)
                obj.put("https", httpsRegistry?.snapshot ?: EMPTY_JSON_ARRAY)
                DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK,
                    Unpooled.copiedBuffer(obj.toString(2), Charsets.UTF_8)
                )
            }
            else -> {
                DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.METHOD_NOT_ALLOWED
                )
            }
        }
    }

}