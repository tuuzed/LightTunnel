package lighttunnel.server.listener

import lighttunnel.base.RemoteConnection

interface OnRemoteConnectionListener {
    fun onRemoteConnected(conn: RemoteConnection) {}
    fun onRemoteDisconnect(conn: RemoteConnection) {}
}
