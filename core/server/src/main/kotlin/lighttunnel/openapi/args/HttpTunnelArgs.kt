package lighttunnel.openapi.args

import lighttunnel.openapi.http.HttpPlugin
import lighttunnel.openapi.http.HttpRequestInterceptor

class HttpTunnelArgs(
    val bindAddr: String? = null,
    val bindPort: Int? = null,
    val httpRequestInterceptor: HttpRequestInterceptor = HttpRequestInterceptor.defaultImpl,
    val httpPlugin: HttpPlugin? = null
)