package lighttunnel.server.args

import lighttunnel.server.TunnelRequestInterceptor

data class TunnelDaemonArgs(
    val bindAddr: String? = null,
    val bindPort: Int = 5080,
    val tunnelRequestInterceptor: TunnelRequestInterceptor? = null
)
