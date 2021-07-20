package lighttunnel.server.tcp

import io.netty.channel.Channel
import lighttunnel.base.TunnelRequest
import lighttunnel.server.traffic.TrafficStats
import lighttunnel.server.utils.SessionChannels

sealed interface TcpFd {
    val tunnelRequest: TunnelRequest
    val connectionCount: Int
    val trafficStats: TrafficStats
}

internal class DefaultTcpFd(
    val sessionChannels: SessionChannels,
    private val closeFuture: () -> Unit
) : TcpFd {

    override val tunnelRequest get() = sessionChannels.tunnelRequest
    override val connectionCount get() = sessionChannels.cachedChannelCount
    override val trafficStats get() = sessionChannels.trafficStats

    val tunnelId get() = sessionChannels.tunnelId
    val port get() = tunnelRequest.remotePort
    val tunnelChannel get() = sessionChannels.tunnelChannel

    fun close() {
        closeFuture()
        sessionChannels.depose()
    }

    fun forceOff() = sessionChannels.forceOff()

    fun putChannel(channel: Channel) = sessionChannels.putChannel(channel)

    fun removeChannel(sessionId: Long) = sessionChannels.removeChannel(sessionId)

    override fun toString(): String = tunnelRequest.toString()

}
