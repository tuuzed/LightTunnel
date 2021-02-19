package lighttunnel.server.listener

import lighttunnel.server.tcp.TcpFd

interface OnTcpTunnelStateListener {
    fun onTcpTunnelConnected(fd: TcpFd)
    fun onTcpTunnelDisconnect(fd: TcpFd)
}
