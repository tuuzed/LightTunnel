package tunnel2.server.http

import tunnel2.server.internal.ServerSessionChannels

class HttpDescriptor(
    val vhost: String,
    val sessionChannels: ServerSessionChannels
) {
    fun close() {
        sessionChannels.destroy()
    }
}