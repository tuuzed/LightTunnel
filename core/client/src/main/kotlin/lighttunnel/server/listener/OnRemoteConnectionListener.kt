package lighttunnel.server.listener

import lighttunnel.base.entity.RemoteConnection

interface OnRemoteConnectionListener {
    fun onRemoteConnected(conn: RemoteConnection) {}
    fun onRemoteDisconnect(conn: RemoteConnection) {}
}
