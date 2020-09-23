package lighttunnel.args

import lighttunnel.TunnelRequestInterceptor

class TunnelDaemonArgs(
    val bindAddr: String? = null,
    val bindPort: Int = 5080,
    val tunnelRequestInterceptor: TunnelRequestInterceptor? = null
)
