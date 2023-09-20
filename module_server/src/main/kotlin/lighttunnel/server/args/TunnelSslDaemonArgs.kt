package lighttunnel.server.args

import io.netty.handler.ssl.SslContext
import lighttunnel.common.utils.SslContextUtils
import lighttunnel.server.TunnelRequestInterceptor

data class TunnelSslDaemonArgs(
    val bindIp: String? = null,
    val bindPort: Int = 7443,
    val tunnelRequestInterceptor: TunnelRequestInterceptor? = null,
    val sslContext: SslContext = SslContextUtils.forBuiltinServer()
)
