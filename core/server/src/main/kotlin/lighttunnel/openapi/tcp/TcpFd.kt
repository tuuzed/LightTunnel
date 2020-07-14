package lighttunnel.openapi.tcp

import lighttunnel.openapi.TunnelRequest
import lighttunnel.openapi.util.Statistics

interface TcpFd {
    val tunnelRequest: TunnelRequest
    val connectionCount: Int
    val statistics: Statistics
}