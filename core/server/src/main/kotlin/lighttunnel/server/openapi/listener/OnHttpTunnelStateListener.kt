package lighttunnel.server.openapi.listener

import lighttunnel.server.openapi.http.HttpFd

interface OnHttpTunnelStateListener {
    fun onHttpTunnelConnected(fd: HttpFd) {}
    fun onHttpTunnelDisconnect(fd: HttpFd) {}
}