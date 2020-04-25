package lighttunnel.server.http

import lighttunnel.server.util.SessionChannels

class HttpFd(
    val host: String,
    val sessionChannels: SessionChannels
) {

    val tunnelId get() = sessionChannels.tunnelId

    val tunnelRequest get() = sessionChannels.tunnelRequest

    val tunnelChannel get() = sessionChannels.tunnelChannel

    val channelCount get() = sessionChannels.cachedChannelCount

    fun close() = sessionChannels.destroy()

}
