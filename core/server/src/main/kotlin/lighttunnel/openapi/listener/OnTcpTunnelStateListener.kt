package lighttunnel.openapi.listener

import lighttunnel.openapi.tcp.TcpFd

interface OnTcpTunnelStateListener {
    fun onTcpTunnelConnected(fd: TcpFd)
    fun onTcpTunnelDisconnect(fd: TcpFd)
}