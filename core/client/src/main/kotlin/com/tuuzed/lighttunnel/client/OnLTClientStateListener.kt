package com.tuuzed.lighttunnel.client

interface OnLTClientStateListener {
    fun onConnecting(descriptor: LTClientDescriptor, reconnect: Boolean) {}
    fun onConnected(descriptor: LTClientDescriptor) {}
    fun onDisconnect(descriptor: LTClientDescriptor, err: Boolean, errCause: Throwable?) {}
}