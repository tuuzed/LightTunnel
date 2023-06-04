package lighttunnel.server

import lighttunnel.common.entity.TunnelRequest
import lighttunnel.common.exception.LightTunnelException

interface TunnelRequestInterceptor {

    @Throws(LightTunnelException::class)
    fun onIntercept(tunnelRequest: TunnelRequest): TunnelRequest = tunnelRequest

}
