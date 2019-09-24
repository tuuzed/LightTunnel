package tpclient

interface OnConnectFailureListener {
    fun onConnectFailure(descriptor: TPClientDescriptor) {}
}