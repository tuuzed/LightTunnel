package lighttunnel.lts.internal

import lighttunnel.common.entity.TunnelRequest
import lighttunnel.common.entity.TunnelType
import lighttunnel.common.exception.LightTunnelException
import lighttunnel.common.utils.PortUtils
import lighttunnel.common.extensions.injectLogger
import lighttunnel.extras.*
import lighttunnel.server.TunnelRequestInterceptor

internal class DefaultTunnelRequestInterceptor(
    /** 预置Token */
    private val authToken: String? = null,
    /** 端口白名单 */
    private val allowPorts: String? = null
) : TunnelRequestInterceptor {
    private val logger by injectLogger()

    @Throws(LightTunnelException::class)
    override fun onIntercept(tunnelRequest: TunnelRequest): TunnelRequest {
        logger.debug("tunnelRequest: ${tunnelRequest.toJsonString()}")
        if (tunnelRequest.tunnelType == TunnelType.UNKNOWN) {
            throw LightTunnelException("TunnelRequest($tunnelRequest), tunnelType == UNKNOWN)")
        }
        if (!authToken.isNullOrEmpty() && authToken != tunnelRequest.authToken) {
            throw LightTunnelException("TunnelRequest($tunnelRequest), Bad Auth Token(${tunnelRequest.authToken})")
        }
        return when (tunnelRequest.tunnelType) {
            TunnelType.TCP -> {
                if (tunnelRequest.remotePort == 0) {
                    tunnelRequest.copyTcp(remotePort = PortUtils.getAvailableTcpPort(allowPorts ?: "1024-65535"))
                } else {
                    if (allowPorts != null && !PortUtils.hasInPortRange(allowPorts, tunnelRequest.remotePort)) {
                        throw LightTunnelException("request($tunnelRequest), remotePort($tunnelRequest.remotePort) Not allowed to use.")
                    }
                    tunnelRequest
                }
            }

            else -> tunnelRequest
        }
    }

}
