package lighttunnel.server.http

import lighttunnel.server.SessionChannels

class HttpDescriptor(
    val host: String,
    val sessionChannels: SessionChannels
) {

    val tunnelId get() = sessionChannels.tunnelId

    val tunnelRequest get() = sessionChannels.tunnelRequest

    val tunnelChannel get() = sessionChannels.tunnelChannel

    val channelCount get() = sessionChannels.cachedChannelCount

    fun close() = sessionChannels.destroy()

}
