package lighttunnel.conn

import lighttunnel.TunnelRequest

interface TunnelConnection {
    val serverAddr: String
    val serverPort: Int
    val tunnelRequest: TunnelRequest
}