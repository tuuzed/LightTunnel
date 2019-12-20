package lighttunnel.client

interface OnConnectFailureListener {
    fun onConnectFailure(descriptor: TunnelConnDescriptor) {}
}