package krp.krp

import krp.common.entity.RemoteConnection
import krp.common.exception.KrpException
import krp.krp.conn.TunnelConn

interface KrpListener {
    fun onTunnelConnecting(conn: TunnelConn, retryConnect: Boolean) {}
    fun onTunnelConnected(conn: TunnelConn) {}
    fun onTunnelDisconnect(conn: TunnelConn, cause: KrpException?) {}
    fun onRemoteConnected(conn: RemoteConnection) {}
    fun onRemoteDisconnect(conn: RemoteConnection) {}
}
