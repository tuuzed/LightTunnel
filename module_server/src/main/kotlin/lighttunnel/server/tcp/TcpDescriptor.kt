package lighttunnel.server.tcp

import io.netty.channel.Channel
import lighttunnel.common.entity.TunnelRequest
import lighttunnel.server.SessionChannels
import lighttunnel.server.TunnelDescriptor
import lighttunnel.server.traffic.TrafficStats

sealed interface TcpDescriptor : TunnelDescriptor {
    val port: Int
    val tunnelRequest: TunnelRequest
    val connectionCount: Int
    val trafficStats: TrafficStats
}

internal class DefaultTcpDescriptor(
    private val sessionChannels: SessionChannels,
    private val closeFuture: () -> Unit,
) : TcpDescriptor {

    override val port get() = tunnelRequest.remotePort
    override val tunnelRequest get() = sessionChannels.tunnelRequest
    override val connectionCount get() = sessionChannels.cachedChannelCount
    override val trafficStats get() = sessionChannels.trafficStats

    val tunnelId get() = sessionChannels.tunnelId
    val tunnelChannel get() = sessionChannels.tunnelChannel

    fun addSessionChannel(channel: Channel) = sessionChannels.addSessionChannel(channel)

    fun removeSessionChannel(sessionId: Long) = sessionChannels.removeSessionChannel(sessionId)

    fun writeAndFlushForceOffMsg() = sessionChannels.writeAndFlushForceOffMsg()

    fun close() {
        closeFuture()
        sessionChannels.depose()
    }

    override fun toString(): String = tunnelRequest.toString()

}
