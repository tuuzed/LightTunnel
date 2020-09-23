package lighttunnel.tcp

import lighttunnel.TunnelRequest
import lighttunnel.util.Statistics

interface TcpFd {
    val tunnelRequest: TunnelRequest
    val connectionCount: Int
    val statistics: Statistics
}