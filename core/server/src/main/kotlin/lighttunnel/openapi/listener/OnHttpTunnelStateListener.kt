package lighttunnel.openapi.listener

import lighttunnel.openapi.http.HttpFd

interface OnHttpTunnelStateListener {
    fun onHttpTunnelConnected(fd: HttpFd)
    fun onHttpTunnelDisconnect(fd: HttpFd)
}