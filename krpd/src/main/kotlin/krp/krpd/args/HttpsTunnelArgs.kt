package krp.krpd.args

import io.netty.handler.ssl.SslContext
import krp.common.utils.SslContextUtils
import krp.krpd.http.HttpPlugin
import krp.krpd.http.HttpTunnelRequestInterceptor

class HttpsTunnelArgs(
    val bindIp: String? = null,
    val bindPort: Int = 443,
    val httpPlugin: HttpPlugin? = null,
    val httpTunnelRequestInterceptor: HttpTunnelRequestInterceptor? = null,
    val sslContext: SslContext = SslContextUtils.forBuiltinServer()
)
