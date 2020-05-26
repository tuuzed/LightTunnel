@file:Suppress("unused")

package lighttunnel.server.tcp

import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import lighttunnel.server.util.SessionChannels

class TcpFd internal constructor(
    val addr: String?,
    val port: Int,
    internal val sessionChannels: SessionChannels,
    private val bindChannelFuture: ChannelFuture
) {

    val tunnelId get() = sessionChannels.tunnelId

    internal val tunnelChannel get() = sessionChannels.tunnelChannel

    internal fun close() {
        bindChannelFuture.channel().close()
        sessionChannels.depose()
    }

    internal fun forcedOffline() = sessionChannels.forcedOffline()

    internal fun putChannel(channel: Channel) = sessionChannels.putChannel(channel)

    internal fun removeChannel(sessionId: Long) = sessionChannels.removeChannel(sessionId)

    override fun toString(): String = sessionChannels.tunnelRequest.toString()

}