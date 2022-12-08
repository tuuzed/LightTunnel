package lighttunnel.client

import lighttunnel.client.conn.TunnelConn
import lighttunnel.common.entity.RemoteConn
import lighttunnel.common.exception.LightTunnelException

interface ClientListener {
    fun onTunnelConnecting(conn: TunnelConn, retryConnect: Boolean) {}
    fun onTunnelConnected(conn: TunnelConn) {}
    fun onTunnelDisconnect(conn: TunnelConn, cause: LightTunnelException?) {}
    fun onRemoteConnected(conn: RemoteConn) {}
    fun onRemoteDisconnect(conn: RemoteConn) {}
}
