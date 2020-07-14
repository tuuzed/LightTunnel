package lighttunnel.client.openapi.conn

import lighttunnel.proto.TunnelRequest

interface TunnelConnection {
    val serverAddr: String
    val serverPort: Int
    val tunnelRequest: TunnelRequest
}