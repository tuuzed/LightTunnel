package lighttunnel.server.http

import io.netty.channel.Channel
import lighttunnel.server.openapi.http.HttpFd
import lighttunnel.server.util.SessionChannels

internal class HttpFdDefaultImpl(
    override val isHttps: Boolean,
    val host: String,
    val sessionChannels: SessionChannels
) : HttpFd {

    override val tunnelRequest get() = sessionChannels.tunnelRequest
    override val connectionCount get() = sessionChannels.cachedChannelCount
    override val statistics get() = sessionChannels.statistics

    val tunnelId get() = sessionChannels.tunnelId

    val tunnelChannel get() = sessionChannels.tunnelChannel

    fun close() = sessionChannels.depose()

    fun forceOff() = sessionChannels.forceOff()

    fun putChannel(channel: Channel) = sessionChannels.putChannel(channel)

    override fun toString(): String = tunnelRequest.toString()


}
