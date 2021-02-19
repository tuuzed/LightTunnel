package lighttunnel.server.tcp

import lighttunnel.base.TunnelRequest
import lighttunnel.server.traffic.TrafficStats

interface TcpFd {
    val tunnelRequest: TunnelRequest
    val connectionCount: Int
    val trafficStats: TrafficStats
}
