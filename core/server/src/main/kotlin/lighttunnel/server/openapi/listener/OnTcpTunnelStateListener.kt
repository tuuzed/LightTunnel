package lighttunnel.server.openapi.listener

import lighttunnel.server.openapi.tcp.TcpFd

interface OnTcpTunnelStateListener {
    fun onTcpTunnelConnected(fd: TcpFd) {}
    fun onTcpTunnelDisconnect(fd: TcpFd) {}
}