package lighttunnel.server.listener

import lighttunnel.server.http.HttpFd

interface OnHttpTunnelStateListener {
    fun onHttpTunnelConnected(fd: HttpFd)
    fun onHttpTunnelDisconnect(fd: HttpFd)
}
