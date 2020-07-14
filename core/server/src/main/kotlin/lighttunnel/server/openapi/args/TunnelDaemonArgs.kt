package lighttunnel.server.openapi.args

import lighttunnel.server.openapi.TunnelRequestInterceptor

class TunnelDaemonArgs(
    val bindAddr: String? = null,
    val bindPort: Int = 5080,
    val tunnelRequestInterceptor: TunnelRequestInterceptor = TunnelRequestInterceptor.emptyImpl
)