package lighttunnel.server.openapi.tcp

import lighttunnel.proto.TunnelRequest
import lighttunnel.server.openapi.util.Statistics

interface TcpFd {
    val tunnelRequest: TunnelRequest
    val connectionCount: Int
    val statistics: Statistics
}