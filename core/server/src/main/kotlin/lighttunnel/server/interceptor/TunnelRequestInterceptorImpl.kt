package lighttunnel.server.interceptor

import lighttunnel.proto.ProtoException
import lighttunnel.proto.TunnelRequest
import lighttunnel.server.util.PortRangeUtil

class TunnelRequestInterceptorImpl(
    /** 预置Token */
    private val authToken: String?,
    /** 端口白名单 */
    private val allowPorts: String?
) : TunnelRequestInterceptor {

    @Throws(ProtoException::class)
    override fun handleTunnelRequest(request: TunnelRequest): TunnelRequest {
        if (authToken != null && authToken != request.authToken) {
            throw ProtoException("request($request), Bad Auth Token(${request.authToken})")
        }
        return when (request.type) {
            TunnelRequest.Type.TCP -> {
                if (request.remotePort == 0) {
                    TunnelRequest.copyTcp(
                        request,
                        remotePort = PortRangeUtil.getAvailableTcpPort(allowPorts ?: "1024-65535")
                    )
                } else {
                    if (allowPorts != null && !PortRangeUtil.hasInPortRange(allowPorts, request.remotePort)) {
                        throw ProtoException("request($request), remotePort($request.remotePort) Not allowed to use.")
                    }
                    request
                }
            }
            else -> {
                request
            }
        }
    }

}
