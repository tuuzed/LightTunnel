package krp.krpd.args

import krp.krpd.TunnelRequestInterceptor

class TunnelDaemonArgs(
    val bindIp: String? = null,
    val bindPort: Int = 7080,
    val tunnelRequestInterceptor: TunnelRequestInterceptor? = null
)
