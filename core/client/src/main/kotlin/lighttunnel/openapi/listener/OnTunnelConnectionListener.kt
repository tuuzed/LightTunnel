package lighttunnel.openapi.listener

import lighttunnel.openapi.conn.TunnelConnection

interface OnTunnelConnectionListener {
    fun onTunnelConnecting(conn: TunnelConnection, retryConnect: Boolean) {}
    fun onTunnelConnected(conn: TunnelConnection) {}
    fun onTunnelDisconnect(conn: TunnelConnection, cause: Throwable?) {}
}