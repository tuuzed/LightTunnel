package lighttunnel.openapi.listener

import lighttunnel.openapi.RemoteConnection

interface OnRemoteConnectionListener {
    fun onRemoteConnected(conn: RemoteConnection) {}
    fun onRemoteDisconnect(conn: RemoteConnection) {}
}