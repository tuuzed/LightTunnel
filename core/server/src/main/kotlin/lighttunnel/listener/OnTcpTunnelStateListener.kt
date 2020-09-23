package lighttunnel.listener

import lighttunnel.tcp.TcpFd

interface OnTcpTunnelStateListener {
    fun onTcpTunnelConnected(fd: TcpFd)
    fun onTcpTunnelDisconnect(fd: TcpFd)
}