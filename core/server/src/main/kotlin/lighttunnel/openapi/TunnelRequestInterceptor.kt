package lighttunnel.openapi

import lighttunnel.server.TunnelRequestInterceptorDefaultImpl

interface TunnelRequestInterceptor {

    @Throws(ProtoException::class)
    fun handleTunnelRequest(tunnelRequest: TunnelRequest): TunnelRequest

    companion object {
        @JvmStatic
        val emptyImpl: TunnelRequestInterceptor by lazy { TunnelRequestInterceptorDefaultImpl() }

        @JvmStatic
        fun defaultImpl(authToken: String? = null, allowPorts: String? = null): TunnelRequestInterceptor = TunnelRequestInterceptorDefaultImpl(authToken, allowPorts)
    }
}
