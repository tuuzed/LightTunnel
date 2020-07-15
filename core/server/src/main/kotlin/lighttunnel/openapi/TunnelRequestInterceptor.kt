package lighttunnel.openapi

interface TunnelRequestInterceptor {

    @Throws(ProtoException::class)
    fun handleTunnelRequest(tunnelRequest: TunnelRequest): TunnelRequest

}
