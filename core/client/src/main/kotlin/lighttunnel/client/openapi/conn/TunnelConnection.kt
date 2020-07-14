package lighttunnel.client.openapi.conn

import lighttunnel.base.openapi.TunnelRequest

interface TunnelConnection {
    val serverAddr: String
    val serverPort: Int
    val tunnelRequest: TunnelRequest
}