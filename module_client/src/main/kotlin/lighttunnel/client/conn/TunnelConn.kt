package lighttunnel.client.conn

import lighttunnel.common.entity.TunnelRequest

sealed interface TunnelConn {
    val serverIp: String
    val serverPort: Int
    val tunnelRequest: TunnelRequest
}
