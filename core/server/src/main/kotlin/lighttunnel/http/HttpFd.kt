package lighttunnel.http

import lighttunnel.TunnelRequest
import lighttunnel.traffic.TrafficStats

interface HttpFd {
    val tunnelRequest: TunnelRequest
    val connectionCount: Int
    val trafficStats: TrafficStats
    val isHttps: Boolean
}