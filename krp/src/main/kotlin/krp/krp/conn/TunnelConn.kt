package krp.krp.conn

import krp.common.entity.TunnelRequest

sealed interface TunnelConn {
    val serverIp: String
    val serverPort: Int
    val tunnelRequest: TunnelRequest
}
