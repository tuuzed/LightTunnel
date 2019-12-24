package lighttunnel.server.interceptor

import lighttunnel.proto.ProtoException
import lighttunnel.proto.TunnelRequest

interface TunnelRequestInterceptor {

    @Throws(ProtoException::class)
    fun handleTunnelRequest(tunnelRequest: TunnelRequest): TunnelRequest = tunnelRequest

}
