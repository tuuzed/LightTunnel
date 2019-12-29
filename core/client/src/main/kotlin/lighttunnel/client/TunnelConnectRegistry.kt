package lighttunnel.client

import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.*
import lighttunnel.api.ApiServer
import lighttunnel.logger.loggerDelegate
import lighttunnel.proto.ProtoException
import org.json.JSONArray
import org.json.JSONObject

class TunnelConnectRegistry : ApiServer.RequestDispatcher {
    private val logger by loggerDelegate()
    private val cachedTunnelConnectDescriptors = ArrayList<TunnelConnectDescriptor>()

    @Synchronized
    @Throws(ProtoException::class)
    fun register(descriptor: TunnelConnectDescriptor) {
        cachedTunnelConnectDescriptors.add(descriptor)
    }

    @Synchronized
    fun unregister(descriptor: TunnelConnectDescriptor) {
        cachedTunnelConnectDescriptors.remove(descriptor)
    }

    @Synchronized
    fun destroy() {
        cachedTunnelConnectDescriptors.forEach { it.close() }
        cachedTunnelConnectDescriptors.clear()
    }

    override fun doRequest(request: FullHttpRequest): FullHttpResponse {
        logger.trace("doRequest: $request")
        return when {
            "/api/status" == request.uri() -> {
                val array = JSONArray()
                cachedTunnelConnectDescriptors.forEach { descriptor ->
                    array.put(
                        JSONObject().also {
                            it.put("closed", descriptor.isClosed)
                            it.put("request", descriptor.tunnelRequest.toString(descriptor.serverAddr))
                            it.put("request_options", descriptor.tunnelRequest.optionsString)
                            it.put("finally_request", descriptor.finallyTunnelRequest?.toString(descriptor.serverAddr))
                            it.put("finally_request_options", descriptor.finallyTunnelRequest?.optionsString)
                        }
                    )
                }
                DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK,
                    Unpooled.copiedBuffer(array.toString(), Charsets.UTF_8)
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