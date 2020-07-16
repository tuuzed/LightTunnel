package lighttunnel.openapi.ext

import lighttunnel.base.util.PortUtil
import lighttunnel.base.util.loggerDelegate
import lighttunnel.openapi.ProtoException
import lighttunnel.openapi.TunnelRequest
import lighttunnel.openapi.TunnelRequestInterceptor
import lighttunnel.openapi.TunnelType

class TunnelRequestInterceptorDefaultImpl(
    /** 预置Token */
    private val authToken: String? = null,
    /** 端口白名单 */
    private val allowPorts: String? = null
) : TunnelRequestInterceptor {
    private val logger by loggerDelegate()

    @Throws(ProtoException::class)
    override fun intercept(tunnelRequest: TunnelRequest): TunnelRequest {
        logger.debug("tunnelRequest: ${tunnelRequest.toRawString()}")
        if (tunnelRequest.tunnelType == TunnelType.UNKNOWN) {
            throw ProtoException("TunnelRequest($tunnelRequest), tunnelType == UNKNOWN)")
        }
        if (!authToken.isNullOrEmpty() && authToken != tunnelRequest.authToken) {
            throw ProtoException("TunnelRequest($tunnelRequest), Bad Auth Token(${tunnelRequest.authToken})")
        }
        return when (tunnelRequest.tunnelType) {
            TunnelType.TCP -> {
                if (tunnelRequest.remotePort == 0) {
                    val port = PortUtil.getAvailableTcpPort(allowPorts ?: "1024-65535")
                    tunnelRequest.copyTcp(remotePort = port)
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