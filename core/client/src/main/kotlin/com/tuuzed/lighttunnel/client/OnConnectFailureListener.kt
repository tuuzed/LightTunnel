package com.tuuzed.lighttunnel.client

interface OnConnectFailureListener {
    fun onConnectFailure(descriptor: LTClientDescriptor) {}
}