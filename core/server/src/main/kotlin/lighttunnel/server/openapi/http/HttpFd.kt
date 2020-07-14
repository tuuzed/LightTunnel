package lighttunnel.server.openapi.http

import lighttunnel.proto.TunnelRequest
import lighttunnel.server.openapi.util.Statistics

interface HttpFd {
    val tunnelRequest: TunnelRequest
    val connectionCount: Int
    val statistics: Statistics
    val isHttps: Boolean
}