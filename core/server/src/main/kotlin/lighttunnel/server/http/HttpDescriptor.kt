package lighttunnel.server.http

import lighttunnel.server.SessionChannels

class HttpDescriptor(
    val host: String,
    val sessionChannels: SessionChannels
) {

    val tunnelId get() = sessionChannels.tunnelId

    fun close() = sessionChannels.destroy()
}
