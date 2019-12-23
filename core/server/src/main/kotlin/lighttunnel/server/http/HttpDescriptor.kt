package lighttunnel.server.http

import lighttunnel.server.SessionChannels

class HttpDescriptor(
    val host: String,
    val sessionPool: SessionChannels
) {
    fun close() = sessionPool.destroy()
}
