package lighttunnel.server.http

import lighttunnel.base.TunnelRequest
import lighttunnel.server.traffic.TrafficStats

interface HttpFd {
    val tunnelRequest: TunnelRequest
    val connectionCount: Int
    val trafficStats: TrafficStats
    val isHttps: Boolean
}
