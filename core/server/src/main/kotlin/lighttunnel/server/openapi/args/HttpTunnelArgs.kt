package lighttunnel.server.openapi.args

import lighttunnel.server.openapi.http.HttpPlugin
import lighttunnel.server.openapi.http.HttpRequestInterceptor

class HttpTunnelArgs(
    val bindAddr: String? = null,
    val bindPort: Int? = null,
    val httpRequestInterceptor: HttpRequestInterceptor = HttpRequestInterceptor.defaultImpl,
    val httpPlugin: HttpPlugin? = null
)