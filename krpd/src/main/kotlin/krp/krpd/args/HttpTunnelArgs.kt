package krp.krpd.args

import krp.krpd.http.HttpPlugin
import krp.krpd.http.HttpTunnelRequestInterceptor

class HttpTunnelArgs(
    val bindIp: String? = null,
    val bindPort: Int = 80,
    val httpPlugin: HttpPlugin? = null,
    val httpTunnelRequestInterceptor: HttpTunnelRequestInterceptor? = null
)
