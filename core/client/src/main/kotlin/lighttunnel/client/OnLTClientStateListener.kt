package lighttunnel.client

interface OnLTClientStateListener {
    fun onConnecting(descriptor: LTConnDescriptor, reconnect: Boolean) {}
    fun onConnected(descriptor: LTConnDescriptor) {}
    fun onDisconnect(descriptor: LTConnDescriptor, err: Boolean, errCause: Throwable?) {}
}