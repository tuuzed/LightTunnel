package lighttunnel.openapi.listener

import lighttunnel.openapi.TunnelRequest

interface OnTrafficListener {

    fun onInbound(tunnelRequest: TunnelRequest, bytes: Int)

    fun onOutbound(tunnelRequest: TunnelRequest, bytes: Int)

}