package lighttunnel.server

import lighttunnel.openapi.TunnelRequest
import lighttunnel.openapi.TunnelRequestInterceptor

internal class TunnelRequestInterceptorEmptyImpl : TunnelRequestInterceptor {

    override fun handleTunnelRequest(tunnelRequest: TunnelRequest): TunnelRequest {
        return tunnelRequest
    }
}