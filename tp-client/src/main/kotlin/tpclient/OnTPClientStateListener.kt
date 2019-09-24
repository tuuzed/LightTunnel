package tpclient

interface OnTPClientStateListener {
    fun onConnecting(descriptor: TPClientDescriptor, reconnect: Boolean) {}
    fun onConnected(descriptor: TPClientDescriptor) {}
    fun onDisconnect(descriptor: TPClientDescriptor, err: Boolean, errCause: Throwable?) {}
}