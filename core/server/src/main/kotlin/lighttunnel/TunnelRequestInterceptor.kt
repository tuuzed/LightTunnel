package lighttunnel

interface TunnelRequestInterceptor {

    @Throws(ProtoException::class)
    fun intercept(tunnelRequest: TunnelRequest): TunnelRequest = tunnelRequest

}
