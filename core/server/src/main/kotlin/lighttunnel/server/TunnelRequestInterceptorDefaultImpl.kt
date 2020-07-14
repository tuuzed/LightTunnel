package lighttunnel.server

import lighttunnel.base.util.PortUtil
import lighttunnel.openapi.ProtoException
import lighttunnel.openapi.TunnelRequest
import lighttunnel.openapi.TunnelRequestInterceptor

internal class TunnelRequestInterceptorDefaultImpl(
    /** 预置Token */
    private val authToken: String? = null,
    /** 端口白名单 */
    private val allowPorts: String? = null
) : TunnelRequestInterceptor {

    @Throws(ProtoException::class)
    override fun handleTunnelRequest(tunnelRequest: TunnelRequest): TunnelRequest {
        if (authToken != null && authToken != tunnelRequest.authToken) {
            throw ProtoException("request($tunnelRequest), Bad Auth Token(${tunnelRequest.authToken})")
        }
        return when (tunnelRequest.type) {
            TunnelRequest.Type.TCP -> {
                if (tunnelRequest.remotePort == 0) {
                    tunnelRequest.copyTcp(
                        remotePort = PortUtil.getAvailableTcpPort(allowPorts ?: "1024-65535")
                    )
                } else {
                    if (allowPorts != null && !PortUtil.hasInPortRange(allowPorts, tunnelRequest.remotePort)) {
                        throw ProtoException("request($tunnelRequest), remotePort($tunnelRequest.remotePort) Not allowed to use.")
                    }
                    tunnelRequest
                }
            }
            else -> tunnelRequest
        }
    }
}