package krp.krpd

import krp.common.entity.TunnelRequest
import krp.krpd.http.HttpFd
import krp.krpd.tcp.TcpFd

interface KrpdListener {
    fun onTcpTunnelConnected(fd: TcpFd) {}
    fun onTcpTunnelDisconnect(fd: TcpFd) {}
    fun onHttpTunnelConnected(fd: HttpFd) {}
    fun onHttpTunnelDisconnect(fd: HttpFd) {}
    fun onTrafficInbound(tunnelRequest: TunnelRequest, bytes: Int) {}
    fun onTrafficOutbound(tunnelRequest: TunnelRequest, bytes: Int) {}
}
