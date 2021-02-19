package lighttunnel.server.http.impl

import io.netty.channel.Channel
import lighttunnel.server.http.HttpFd
import lighttunnel.server.utils.SessionChannels

internal class HttpFdImpl(
    override val isHttps: Boolean,
    val sessionChannels: SessionChannels
) : HttpFd {

    override val tunnelRequest get() = sessionChannels.tunnelRequest
    override val connectionCount get() = sessionChannels.cachedChannelCount
    override val trafficStats get() = sessionChannels.trafficStatsDefaultImpl

    val tunnelId get() = sessionChannels.tunnelId
    val host get() = tunnelRequest.host
    val tunnelChannel get() = sessionChannels.tunnelChannel

    fun close() = sessionChannels.depose()

    fun forceOff() = sessionChannels.forceOff()

    fun putChannel(channel: Channel) = sessionChannels.putChannel(channel)

    override fun toString(): String = tunnelRequest.toString()


}
