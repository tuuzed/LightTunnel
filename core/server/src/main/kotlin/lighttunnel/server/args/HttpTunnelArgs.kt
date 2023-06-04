package lighttunnel.server.args

import lighttunnel.server.http.HttpPlugin
import lighttunnel.server.http.HttpTunnelRequestInterceptor

data class HttpTunnelArgs(
    val bindIp: String? = null,
    val bindPort: Int = 80,
    val httpPlugin: HttpPlugin? = null,
    val httpTunnelRequestInterceptor: HttpTunnelRequestInterceptor? = null
)
