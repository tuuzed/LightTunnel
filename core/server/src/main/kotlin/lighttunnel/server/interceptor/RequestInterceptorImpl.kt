package lighttunnel.server.interceptor

import lighttunnel.proto.ProtoException
import lighttunnel.proto.ProtoRequest
import lighttunnel.server.util.hasInPortRange

class RequestInterceptorImpl(
    /** 预置Token */
    private val authToken: String?,
    /** 端口白名单 */
    private val allowPorts: String?
) : RequestInterceptor {

    @Throws(ProtoException::class)
    override fun handleTPRequest(request: ProtoRequest): ProtoRequest {
        if (authToken != null && authToken != request.authToken) {
            throw ProtoException("request($request), Bad Auth Token(${request.authToken})")
        }
        return when (request.type) {
            ProtoRequest.Type.TCP -> {
                val remotePort = request.remotePort
                if (allowPorts != null && !hasInPortRange(allowPorts, remotePort)) {
                    throw ProtoException("request($request), remotePort($remotePort) Not allowed to use.")
                }
                request
            }
            else -> request
        }
    }
}
