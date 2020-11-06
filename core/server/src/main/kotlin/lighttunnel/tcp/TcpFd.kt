package lighttunnel.tcp

import lighttunnel.TunnelRequest
import lighttunnel.traffic.TrafficStats

interface TcpFd {
    val tunnelRequest: TunnelRequest
    val connectionCount: Int
    val trafficStats: TrafficStats
}