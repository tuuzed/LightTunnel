package lighttunnel.ext

import lighttunnel.ProtoException
import lighttunnel.TunnelRequest
import lighttunnel.TunnelRequestInterceptor
import lighttunnel.TunnelType
import lighttunnel.internal.base.util.PortUtil
import lighttunnel.internal.base.util.loggerDelegate

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
