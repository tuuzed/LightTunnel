package lighttunnel.listener

import lighttunnel.TunnelRequest

interface OnTrafficListener {

    fun onInbound(tunnelRequest: TunnelRequest, bytes: Int)

    fun onOutbound(tunnelRequest: TunnelRequest, bytes: Int)

}