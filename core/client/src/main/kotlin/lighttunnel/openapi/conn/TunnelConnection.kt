package lighttunnel.openapi.conn

import lighttunnel.openapi.TunnelRequest

interface TunnelConnection {
    val serverAddr: String
    val serverPort: Int
    val tunnelRequest: TunnelRequest
}