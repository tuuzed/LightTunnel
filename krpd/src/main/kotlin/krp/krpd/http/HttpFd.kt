package krp.krpd.http

import io.netty.channel.Channel
import krp.common.entity.TunnelRequest
import krp.krpd.SessionChannels
import krp.krpd.TunnelFd
import krp.krpd.traffic.TrafficStats

sealed interface HttpFd : TunnelFd {
    val vhost: String
    val tunnelRequest: TunnelRequest
    val connectionCount: Int
    val isHttps: Boolean
    val trafficStats: TrafficStats
}

internal class DefaultHttpFd(
    override val isHttps: Boolean,
    private val sessionChannels: SessionChannels,
) : HttpFd {

    override val vhost get() = tunnelRequest.vhost
    override val tunnelRequest get() = sessionChannels.tunnelRequest
    override val connectionCount get() = sessionChannels.cachedChannelCount
    override val trafficStats get() = sessionChannels.trafficStats

    val tunnelId get() = sessionChannels.tunnelId
    val tunnelChannel get() = sessionChannels.tunnelChannel

    fun putSessionChannel(channel: Channel) = sessionChannels.putSessionChannel(channel)

    fun writeAndFlushForceOffMsg() = sessionChannels.writeAndFlushForceOffMsg()

    fun close() = sessionChannels.depose()

    override fun toString(): String = tunnelRequest.toString()

}
