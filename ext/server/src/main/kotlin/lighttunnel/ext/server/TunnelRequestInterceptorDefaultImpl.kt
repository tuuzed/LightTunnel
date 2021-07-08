package lighttunnel.ext.server

import lighttunnel.base.TunnelRequest
import lighttunnel.base.TunnelType
import lighttunnel.base.proto.ProtoException
import lighttunnel.base.utils.PortUtils
import lighttunnel.base.utils.loggerDelegate
import lighttunnel.ext.base.authToken
import lighttunnel.server.TunnelRequestInterceptor

class TunnelRequestInterceptorDefaultImpl(
    /** 预置Token */
    private val authToken: String? = null,
    /** 端口白名单 */
    private val allowPorts: String? = null
) : TunnelRequestInterceptor {
    private val logger by loggerDelegate()

    @Throws(ProtoException::class)
    override fun intercept(tunnelRequest: TunnelRequest): TunnelRequest {
        logger.debug("tunnelRequest: ${tunnelRequest.toString_()}")
        if (tunnelRequest.tunnelType == TunnelType.UNKNOWN) {
            throw ProtoException("TunnelRequest($tunnelRequest), tunnelType == UNKNOWN)")
        }
        if (!authToken.isNullOrEmpty() && authToken != tunnelRequest.authToken) {
            throw ProtoException("TunnelRequest($tunnelRequest), Bad Auth Token(${tunnelRequest.authToken})")
        }
        return when (tunnelRequest.tunnelType) {
            TunnelType.TCP -> {
                if (tunnelRequest.remotePort == 0) {
                    val port = PortUtils.getAvailableTcpPort(allowPorts ?: "1024-65535")
                    tunnelRequest.copyTcp(remotePort = port)
                } else {
                    if (allowPorts != null && !PortUtils.hasInPortRange(allowPorts, tunnelRequest.remotePort)) {
                        throw ProtoException("request($tunnelRequest), remotePort($tunnelRequest.remotePort) Not allowed to use.")
                    }
                    tunnelRequest
                }
            }
            else -> tunnelRequest
        }
    }

}
