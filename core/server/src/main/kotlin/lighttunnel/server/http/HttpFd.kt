package lighttunnel.server.http

import io.netty.channel.Channel
import lighttunnel.server.util.SessionChannels

class HttpFd internal constructor(
    internal val host: String,
    internal val sessionChannels: SessionChannels
) {

    internal val tunnelId get() = sessionChannels.tunnelId

    internal val tunnelRequest get() = sessionChannels.tunnelRequest

    internal val tunnelChannel get() = sessionChannels.tunnelChannel

    internal fun close() = sessionChannels.depose()

    internal fun forceOff() = sessionChannels.forceOff()

    internal fun putChannel(channel: Channel) = sessionChannels.putChannel(channel)

    override fun toString(): String = sessionChannels.tunnelRequest.toString()


}
