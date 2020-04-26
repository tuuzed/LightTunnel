package lighttunnel.client.callback

import lighttunnel.client.connect.TunnelConnectFd

interface OnTunnelStateListener {
    fun onConnecting(fd: TunnelConnectFd, reconnect: Boolean) {}
    fun onConnected(fd: TunnelConnectFd) {}
    fun onDisconnect(fd: TunnelConnectFd, err: Boolean, errCause: Throwable?) {}
}