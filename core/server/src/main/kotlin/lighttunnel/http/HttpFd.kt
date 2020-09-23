package lighttunnel.http

import lighttunnel.TunnelRequest
import lighttunnel.util.Statistics

interface HttpFd {
    val tunnelRequest: TunnelRequest
    val connectionCount: Int
    val statistics: Statistics
    val isHttps: Boolean
}