package lighttunnel.server.http

import io.netty.channel.Channel
import lighttunnel.server.util.SessionChannels

class HttpFd private constructor(
    val isHttps: Boolean,
    val host: String,
    internal val sessionChannels: SessionChannels
) {

    val tunnelRequest get() = sessionChannels.tunnelRequest
    val connectionCount get() = sessionChannels.cachedChannelCount
    val statistics get() = sessionChannels.statistics

    @Suppress("ClassName")
    internal companion object `-Companion` {
        fun newInstance(
            isHttps: Boolean,
            host: String,
            sessionChannels: SessionChannels
        ) = HttpFd(isHttps, host, sessionChannels)
    }

    internal val tunnelId get() = sessionChannels.tunnelId

    internal val tunnelChannel get() = sessionChannels.tunnelChannel

    internal fun close() = sessionChannels.depose()

    internal fun forceOff() = sessionChannels.forceOff()

    internal fun putChannel(channel: Channel) = sessionChannels.putChannel(channel)

    override fun toString(): String = tunnelRequest.toString()


}
