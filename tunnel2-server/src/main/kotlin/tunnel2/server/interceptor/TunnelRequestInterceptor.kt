package tunnel2.server.interceptor

import tunnel2.common.TunnelException
import tunnel2.common.TunnelRequest

interface TunnelRequestInterceptor {

    companion object {
        val EMPTY_IMPL = object : TunnelRequestInterceptor {}
    }

    @Throws(TunnelException::class)
    fun handleTunnelRequest(request: TunnelRequest): TunnelRequest = request

}
