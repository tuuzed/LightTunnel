package krp.krpd

import krp.common.entity.TunnelRequest
import krp.common.exception.KrpException

interface TunnelRequestInterceptor {

    @Throws(KrpException::class)
    fun intercept(tunnelRequest: TunnelRequest): TunnelRequest = tunnelRequest

}
