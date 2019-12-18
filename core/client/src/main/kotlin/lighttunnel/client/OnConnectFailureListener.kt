package lighttunnel.client

interface OnConnectFailureListener {
    fun onConnectFailure(descriptor: LTConnDescriptor) {}
}