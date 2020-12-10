package lighttunnel.listener

import lighttunnel.RemoteConnection

interface OnRemoteConnectionListener {
    fun onRemoteConnected(conn: RemoteConnection) {}
    fun onRemoteDisconnect(conn: RemoteConnection) {}
}