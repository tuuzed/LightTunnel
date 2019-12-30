package lighttunnel.client

import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.*
import lighttunnel.api.ApiServer

class DashRequestDispatcher(
    private val registry: TunnelConnectRegistry
) : ApiServer.RequestDispatcher {

    override fun doRequest(request: FullHttpRequest): FullHttpResponse {
        return when {
            "/api/snapshot" == request.uri() -> {
                DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK,
                    Unpooled.copiedBuffer(registry.snapshot.toString(2), Charsets.UTF_8)
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