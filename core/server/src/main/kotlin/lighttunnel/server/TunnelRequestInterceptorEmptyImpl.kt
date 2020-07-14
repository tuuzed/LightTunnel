package lighttunnel.server

import lighttunnel.base.openapi.TunnelRequest
import lighttunnel.server.openapi.TunnelRequestInterceptor

internal class TunnelRequestInterceptorEmptyImpl : TunnelRequestInterceptor {

    override fun handleTunnelRequest(tunnelRequest: TunnelRequest): TunnelRequest {
        return tunnelRequest
    }
}