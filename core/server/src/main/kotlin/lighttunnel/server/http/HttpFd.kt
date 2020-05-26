package lighttunnel.server.http

import lighttunnel.server.util.SessionChannels

class HttpFd internal constructor(
    val host: String,
    internal val sessionChannels: SessionChannels
) {

    val tunnelId get() = sessionChannels.tunnelId

    val tunnelRequest get() = sessionChannels.tunnelRequest

    internal val tunnelChannel get() = sessionChannels.tunnelChannel

    val channelCount get() = sessionChannels.cachedChannelCount

    internal fun close() = sessionChannels.depose()

    override fun toString(): String {
        return tunnelRequest.toString()
    }

}
