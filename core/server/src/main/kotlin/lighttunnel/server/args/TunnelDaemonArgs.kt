package lighttunnel.server.args

import lighttunnel.server.TunnelRequestInterceptor

class TunnelDaemonArgs(
    val bindIp: String? = null,
    val bindPort: Int = 7080,
    val tunnelRequestInterceptor: TunnelRequestInterceptor? = null
)
