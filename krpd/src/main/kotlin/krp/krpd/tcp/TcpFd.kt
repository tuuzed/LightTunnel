package krp.krpd.tcp

import io.netty.channel.Channel
import krp.common.entity.TunnelRequest
import krp.krpd.SessionChannels
import krp.krpd.TunnelFd
import krp.krpd.traffic.TrafficStats

sealed interface TcpFd : TunnelFd {
    val port: Int
    val tunnelRequest: TunnelRequest
    val connectionCount: Int
    val trafficStats: TrafficStats
}

internal class DefaultTcpFd(
    private val sessionChannels: SessionChannels,
    private val closeFuture: () -> Unit,
) : TcpFd {

    override val port get() = tunnelRequest.remotePort
    override val tunnelRequest get() = sessionChannels.tunnelRequest
    override val connectionCount get() = sessionChannels.cachedChannelCount
    override val trafficStats get() = sessionChannels.trafficStats

    val tunnelId get() = sessionChannels.tunnelId
    val tunnelChannel get() = sessionChannels.tunnelChannel

    fun putSessionChannel(channel: Channel) = sessionChannels.putSessionChannel(channel)

    fun removeSessionChannel(sessionId: Long) = sessionChannels.removeSessionChannel(sessionId)

    fun writeAndFlushForceOffMsg() = sessionChannels.writeAndFlushForceOffMsg()

    fun close() {
        closeFuture()
        sessionChannels.depose()
    }

    override fun toString(): String = tunnelRequest.toString()

}
