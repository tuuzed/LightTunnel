package lighttunnel.server.openapi.args

import io.netty.handler.ssl.SslContext
import lighttunnel.server.openapi.http.HttpPlugin
import lighttunnel.server.openapi.http.HttpRequestInterceptor
import lighttunnel.util.SslContextUtil

class HttpsTunnelArgs(
    val bindAddr: String? = null,
    val bindPort: Int? = null,
    val httpRequestInterceptor: HttpRequestInterceptor = HttpRequestInterceptor.defaultImpl,
    val httpPlugin: HttpPlugin? = null,
    val sslContext: SslContext = SslContextUtil.forBuiltinServer()
)