package lighttunnel.server

import lighttunnel.common.entity.TunnelRequest
import lighttunnel.common.exception.LightTunnelException

interface TunnelRequestInterceptor {

    @Throws(LightTunnelException::class)
    fun intercept(tunnelRequest: TunnelRequest): TunnelRequest = tunnelRequest

}
