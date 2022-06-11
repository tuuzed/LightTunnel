package krp.krpd.cli.internal

import krp.common.entity.TunnelRequest
import krp.common.entity.TunnelType
import krp.common.exception.KrpException
import krp.common.utils.PortUtils
import krp.common.utils.injectLogger
import krp.extensions.authToken
import krp.krpd.TunnelRequestInterceptor

internal class DefaultTunnelRequestInterceptor(
    /** 预置Token */
    private val authToken: String? = null,
    /** 端口白名单 */
    private val allowPorts: String? = null
) : TunnelRequestInterceptor {
    private val logger by injectLogger()

    @Throws(KrpException::class)
    override fun intercept(tunnelRequest: TunnelRequest): TunnelRequest {
        logger.debug("tunnelRequest: ${tunnelRequest.asJsonString()}")
        if (tunnelRequest.tunnelType == TunnelType.UNKNOWN) {
            throw KrpException("TunnelRequest($tunnelRequest), tunnelType == UNKNOWN)")
        }
        if (!authToken.isNullOrEmpty() && authToken != tunnelRequest.authToken) {
            throw KrpException("TunnelRequest($tunnelRequest), Bad Auth Token(${tunnelRequest.authToken})")
        }
        return when (tunnelRequest.tunnelType) {
            TunnelType.TCP -> {
                if (tunnelRequest.remotePort == 0) {
                    tunnelRequest.copyTcp(remotePort = PortUtils.getAvailableTcpPort(allowPorts ?: "1024-65535"))
                } else {
                    if (allowPorts != null && !PortUtils.hasInPortRange(allowPorts, tunnelRequest.remotePort)) {
                        throw KrpException("request($tunnelRequest), remotePort($tunnelRequest.remotePort) Not allowed to use.")
                    }
                    tunnelRequest
                }
            }
            else -> tunnelRequest
        }
    }

}
