package lighttunnel.server.http

import io.netty.channel.Channel
import lighttunnel.common.entity.TunnelRequest
import lighttunnel.server.SessionChannels
import lighttunnel.server.TunnelDescriptor
import lighttunnel.server.traffic.TrafficStats

sealed interface HttpDescriptor : TunnelDescriptor {
    val vhost: String
    val tunnelRequest: TunnelRequest
    val connectionCount: Int
    val isHttps: Boolean
    val trafficStats: TrafficStats
}

internal class DefaultHttpDescriptor(
    override val isHttps: Boolean,
    private val sessionChannels: SessionChannels,
) : HttpDescriptor {

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
