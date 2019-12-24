package lighttunnel.server.interceptor

import lighttunnel.proto.ProtoException
import lighttunnel.proto.TunnelRequest
import lighttunnel.server.util.PortRangeUtil

internal class TunnelRequestInterceptorImpl(
    /** 预置Token */
    private val authToken: String?,
    /** 端口白名单 */
    private val allowPorts: String?
) : TunnelRequestInterceptor {

    @Throws(ProtoException::class)
    override fun handleTunnelRequest(tunnelRequest: TunnelRequest): TunnelRequest {
        if (authToken != null && authToken != tunnelRequest.authToken) {
            throw ProtoException("request($tunnelRequest), Bad Auth Token(${tunnelRequest.authToken})")
        }
        return when (tunnelRequest.type) {
            TunnelRequest.Type.TCP -> {
                if (tunnelRequest.remotePort == 0) {
                    TunnelRequest.copyTcp(
                        tunnelRequest,
                        remotePort = PortRangeUtil.getAvailableTcpPort(allowPorts ?: "1024-65535")
                    )
                } else {
                    if (allowPorts != null && !PortRangeUtil.hasInPortRange(allowPorts, tunnelRequest.remotePort)) {
                        throw ProtoException("request($tunnelRequest), remotePort($tunnelRequest.remotePort) Not allowed to use.")
                    }
                    tunnelRequest
                }
            }
            else -> {
                tunnelRequest
            }
        }
    }

}
