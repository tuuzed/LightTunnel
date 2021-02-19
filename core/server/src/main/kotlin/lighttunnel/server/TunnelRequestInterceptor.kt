package lighttunnel.server

import lighttunnel.base.TunnelRequest
import lighttunnel.base.proto.ProtoException

interface TunnelRequestInterceptor {

    @Throws(ProtoException::class)
    fun intercept(tunnelRequest: TunnelRequest): TunnelRequest = tunnelRequest

}
