package lighttunnel.server.listener

import lighttunnel.server.conn.TunnelConn

interface OnTunnelConnectionListener {
    fun onTunnelConnecting(conn: TunnelConn, retryConnect: Boolean) {}
    fun onTunnelConnected(conn: TunnelConn) {}
    fun onTunnelDisconnect(conn: TunnelConn, cause: Throwable?) {}
}
