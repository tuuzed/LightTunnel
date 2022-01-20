package lighttunnel.server.listener

import lighttunnel.base.entity.TunnelRequest

interface OnTrafficListener {

    fun onInbound(tunnelRequest: TunnelRequest, bytes: Int)

    fun onOutbound(tunnelRequest: TunnelRequest, bytes: Int)

}
