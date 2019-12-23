package lighttunnel.server.interceptor

import lighttunnel.proto.ProtoException
import lighttunnel.proto.TunnelRequest

interface TunnelRequestInterceptor {

    companion object {
        val emptyImpl by lazy {
            object : TunnelRequestInterceptor {
                override fun handleTunnelRequest(
                    request: TunnelRequest
                ): TunnelRequest = request
            }
        }
    }

    @Throws(ProtoException::class)
    fun handleTunnelRequest(request: TunnelRequest): TunnelRequest

}
