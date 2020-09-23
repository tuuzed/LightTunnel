package lighttunnel.args

import lighttunnel.http.HttpPlugin
import lighttunnel.http.HttpTunnelRequestInterceptor

class HttpTunnelArgs(
    val bindAddr: String? = null,
    val bindPort: Int = 80,
    val httpPlugin: HttpPlugin? = null,
    val httpTunnelRequestInterceptor: HttpTunnelRequestInterceptor? = null
)