package lighttunnel.openapi.http

import lighttunnel.openapi.TunnelRequest
import lighttunnel.openapi.util.Statistics

interface HttpFd {
    val tunnelRequest: TunnelRequest
    val connectionCount: Int
    val statistics: Statistics
    val isHttps: Boolean
}