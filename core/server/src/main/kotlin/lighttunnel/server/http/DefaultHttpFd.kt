package lighttunnel.server.http

import io.netty.channel.Channel
import lighttunnel.server.utils.SessionChannels

internal class DefaultHttpFd(
    override val isHttps: Boolean,
    val sessionChannels: SessionChannels
) : HttpFd {

    override val tunnelRequest get() = sessionChannels.tunnelRequest
    override val connectionCount get() = sessionChannels.cachedChannelCount
    override val trafficStats get() = sessionChannels.trafficStats

    val tunnelId get() = sessionChannels.tunnelId
    val host get() = tunnelRequest.host
    val tunnelChannel get() = sessionChannels.tunnelChannel

    fun close() = sessionChannels.depose()

    fun forceOff() = sessionChannels.forceOff()

    fun putChannel(channel: Channel) = sessionChannels.putChannel(channel)

    override fun toString(): String = tunnelRequest.toString()


}
