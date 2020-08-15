package lighttunnel.openapi.args

import io.netty.handler.ssl.SslContext
import lighttunnel.openapi.SslContextUtil
import lighttunnel.openapi.http.HttpPlugin
import lighttunnel.openapi.http.HttpTunnelRequestInterceptor

class HttpsTunnelArgs(
    val bindAddr: String? = null,
    val bindPort: Int = 443,
    val maxContentLength: Int = 1024 * 1024 * 8,
    val httpPlugin: HttpPlugin? = null,
    val httpTunnelRequestInterceptor: HttpTunnelRequestInterceptor? = null,
    val sslContext: SslContext = SslContextUtil.forBuiltinServer()
)