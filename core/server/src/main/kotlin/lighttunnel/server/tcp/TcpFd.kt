@file:Suppress("unused")

package lighttunnel.server.tcp

import io.netty.channel.Channel
import lighttunnel.server.util.SessionChannels

class TcpFd private constructor(
    val addr: String?,
    val port: Int,
    internal val sessionChannels: SessionChannels,
    private val closeFuture: () -> Unit
) {

    val tunnelRequest get() = sessionChannels.tunnelRequest
    val connectionCount get() = sessionChannels.cachedChannelCount
    val statistics get() = sessionChannels.statistics


    @Suppress("ClassName")
    internal companion object `-Companion` {
        fun newInstance(
            addr: String?,
            port: Int,
            sessionChannels: SessionChannels,
            closeFuture: () -> Unit
        ) = TcpFd(addr, port, sessionChannels, closeFuture)
    }

    internal val tunnelId get() = sessionChannels.tunnelId

    internal val tunnelChannel get() = sessionChannels.tunnelChannel

    internal fun close() {
        closeFuture()
        sessionChannels.depose()
    }

    internal fun forceOff() = sessionChannels.forceOff()

    internal fun putChannel(channel: Channel) = sessionChannels.putChannel(channel)

    internal fun removeChannel(sessionId: Long) = sessionChannels.removeChannel(sessionId)

    override fun toString(): String = tunnelRequest.toString()

}