package lighttunnel.server.args

import io.netty.handler.ssl.SslContext
import lighttunnel.base.utils.SslContextUtils
import lighttunnel.server.TunnelRequestInterceptor

data class SslTunnelDaemonArgs(
    val bindAddr: String? = null,
    val bindPort: Int = 5443,
    val tunnelRequestInterceptor: TunnelRequestInterceptor? = null,
    val sslContext: SslContext = SslContextUtils.forBuiltinServer()
)
