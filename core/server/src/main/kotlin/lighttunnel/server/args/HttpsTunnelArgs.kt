package lighttunnel.server.args

import io.netty.handler.ssl.SslContext
import lighttunnel.base.utils.SslContextUtils
import lighttunnel.server.http.HttpPlugin
import lighttunnel.server.http.HttpTunnelRequestInterceptor

data class HttpsTunnelArgs(
    val bindAddr: String? = null,
    val bindPort: Int = 443,
    val httpPlugin: HttpPlugin? = null,
    val httpTunnelRequestInterceptor: HttpTunnelRequestInterceptor? = null,
    val sslContext: SslContext = SslContextUtils.forBuiltinServer()
)
