package lighttunnel.server.openapi.tcp

import lighttunnel.base.openapi.TunnelRequest
import lighttunnel.server.openapi.util.Statistics

interface TcpFd {
    val tunnelRequest: TunnelRequest
    val connectionCount: Int
    val statistics: Statistics
}