package lighttunnel.openapi

interface TunnelRequestInterceptor {

    @Throws(ProtoException::class)
    fun intercept(tunnelRequest: TunnelRequest): TunnelRequest

}
