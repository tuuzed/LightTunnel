@file:Suppress("unused")

package lighttunnel.server.tcp

import io.netty.channel.ChannelFuture
import lighttunnel.server.util.SessionChannels

class TcpFd internal constructor(
    val addr: String?,
    val port: Int,
    internal val sessionChannels: SessionChannels,
    private val bindChannelFuture: ChannelFuture
) {

    val tunnelId get() = sessionChannels.tunnelId

    val tunnelRequest get() = sessionChannels.tunnelRequest

    internal val tunnelChannel get() = sessionChannels.tunnelChannel

    val channelCount get() = sessionChannels.cachedChannelCount

    internal fun close() {
        bindChannelFuture.channel().close()
        sessionChannels.depose()
    }

    override fun toString(): String {
        return tunnelRequest.toString()
    }

}