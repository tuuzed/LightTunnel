package lighttunnel.client.callback

import lighttunnel.client.connect.TunnelConnectDescriptor

interface OnTunnelStateListener {
    fun onConnecting(descriptor: TunnelConnectDescriptor, reconnect: Boolean) {}
    fun onConnected(descriptor: TunnelConnectDescriptor) {}
    fun onDisconnect(descriptor: TunnelConnectDescriptor, err: Boolean, errCause: Throwable?) {}
}