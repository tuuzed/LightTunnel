package lighttunnel.listener

import lighttunnel.http.HttpFd

interface OnHttpTunnelStateListener {
    fun onHttpTunnelConnected(fd: HttpFd)
    fun onHttpTunnelDisconnect(fd: HttpFd)
}