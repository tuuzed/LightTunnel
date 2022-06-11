package krp.krpd.args

import io.netty.handler.ssl.SslContext
import krp.common.utils.SslContextUtils
import krp.krpd.TunnelRequestInterceptor

class TunnelSslDaemonArgs(
    val bindIp: String? = null,
    val bindPort: Int = 7443,
    val tunnelRequestInterceptor: TunnelRequestInterceptor? = null,
    val sslContext: SslContext = SslContextUtils.forBuiltinServer()
)
