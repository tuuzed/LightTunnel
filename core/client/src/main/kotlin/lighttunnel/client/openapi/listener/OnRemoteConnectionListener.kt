package lighttunnel.client.openapi.listener

import lighttunnel.proto.RemoteConnection

interface OnRemoteConnectionListener {
    fun onRemoteConnected(conn: RemoteConnection) {}
    fun onRemoteDisconnect(conn: RemoteConnection) {}
}