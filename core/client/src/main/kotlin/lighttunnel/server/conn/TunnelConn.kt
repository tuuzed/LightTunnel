package lighttunnel.server.conn

import lighttunnel.base.TunnelRequest

interface TunnelConn {
    val serverAddr: String
    val serverPort: Int
    val tunnelRequest: TunnelRequest
}
