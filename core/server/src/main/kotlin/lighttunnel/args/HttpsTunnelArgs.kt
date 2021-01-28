package lighttunnel.args

import io.netty.handler.ssl.SslContext
import lighttunnel.SslContextUtils
import lighttunnel.http.HttpPlugin
import lighttunnel.http.HttpTunnelRequestInterceptor

class HttpsTunnelArgs(
    val bindAddr: String? = null,
    val bindPort: Int = 443,
    val httpPlugin: HttpPlugin? = null,
    val httpTunnelRequestInterceptor: HttpTunnelRequestInterceptor? = null,
    val sslContext: SslContext = SslContextUtils.forBuiltinServer()
)
