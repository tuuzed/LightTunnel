package lighttunnel.client

interface OnTunnelClientStateListener {
    fun onConnecting(descriptor: TunnelConnDescriptor, reconnect: Boolean) {}
    fun onConnected(descriptor: TunnelConnDescriptor) {}
    fun onDisconnect(descriptor: TunnelConnDescriptor, err: Boolean, errCause: Throwable?) {}
}