package lighttunnel.openapi.args

import io.netty.handler.ssl.SslContext
import lighttunnel.openapi.SslContextUtil
import lighttunnel.openapi.TunnelRequestInterceptor

class SslTunnelDaemonArgs(
    val bindAddr: String? = null,
    val bindPort: Int = 5443,
    val tunnelRequestInterceptor: TunnelRequestInterceptor? = null,
    val sslContext: SslContext = SslContextUtil.forBuiltinServer()
)