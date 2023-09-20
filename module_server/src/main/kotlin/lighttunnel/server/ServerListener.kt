package lighttunnel.server

import lighttunnel.common.entity.TunnelRequest
import lighttunnel.server.http.HttpDescriptor
import lighttunnel.server.tcp.TcpDescriptor

interface ServerListener {
    fun onTcpTunnelConnected(descriptor: TcpDescriptor) {}
    fun onTcpTunnelDisconnect(descriptor: TcpDescriptor) {}
    fun onHttpTunnelConnected(descriptor: HttpDescriptor) {}
    fun onHttpTunnelDisconnect(descriptor: HttpDescriptor) {}
    fun onTrafficInbound(tunnelRequest: TunnelRequest, bytes: Int) {}
    fun onTrafficOutbound(tunnelRequest: TunnelRequest, bytes: Int) {}
}
