package lighttunnel.openapi

interface TunnelRequestInterceptor {

    @Throws(ProtoException::class)
    fun handleTunnelRequest(tunnelRequest: TunnelRequest): TunnelRequest

    companion object {
        @JvmStatic
        val emptyImpl: TunnelRequestInterceptor by lazy {
            object : TunnelRequestInterceptor {
                override fun handleTunnelRequest(tunnelRequest: TunnelRequest) = tunnelRequest
            }
        }
    }
}
