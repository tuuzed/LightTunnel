package lighttunnel.server.openapi

import lighttunnel.base.openapi.ProtoException
import lighttunnel.base.openapi.TunnelRequest
import lighttunnel.server.TunnelRequestInterceptorDefaultImpl

interface TunnelRequestInterceptor {

    @Throws(ProtoException::class)
    fun handleTunnelRequest(tunnelRequest: TunnelRequest): TunnelRequest

    companion object {
        val emptyImpl: TunnelRequestInterceptor by lazy { TunnelRequestInterceptorDefaultImpl() }
        fun defaultImpl(authToken: String? = null, allowPorts: String? = null): TunnelRequestInterceptor = TunnelRequestInterceptorDefaultImpl(authToken, allowPorts)
    }
}
