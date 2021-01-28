package lighttunnel.args

import io.netty.handler.ssl.SslContext
import lighttunnel.SslContextUtils
import lighttunnel.TunnelRequestInterceptor

class SslTunnelDaemonArgs(
    val bindAddr: String? = null,
    val bindPort: Int = 5443,
    val tunnelRequestInterceptor: TunnelRequestInterceptor? = null,
    val sslContext: SslContext = SslContextUtils.forBuiltinServer()
)
