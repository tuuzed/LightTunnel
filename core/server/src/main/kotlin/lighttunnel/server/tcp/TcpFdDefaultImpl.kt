@file:Suppress("unused")

package lighttunnel.server.tcp

import io.netty.channel.Channel
import lighttunnel.openapi.tcp.TcpFd
import lighttunnel.server.util.SessionChannels

internal class TcpFdDefaultImpl(
    val sessionChannels: SessionChannels,
    private val closeFuture: () -> Unit
) : TcpFd {

    override val tunnelRequest get() = sessionChannels.tunnelRequest
    override val connectionCount get() = sessionChannels.cachedChannelCount
    override val statistics get() = sessionChannels.statistics

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