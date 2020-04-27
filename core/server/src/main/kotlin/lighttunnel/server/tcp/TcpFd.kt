@file:Suppress("unused")

package lighttunnel.server.tcp

import io.netty.channel.ChannelFuture
import lighttunnel.server.util.SessionChannels

class TcpFd(
    val addr: String?,
    val port: Int,
    val sessionChannels: SessionChannels,
    private val bindChannelFuture: ChannelFuture
) {

    val tunnelId get() = sessionChannels.tunnelId

    val tunnelRequest get() = sessionChannels.tunnelRequest

    val tunnelChannel get() = sessionChannels.tunnelChannel

    val channelCount get() = sessionChannels.cachedChannelCount

    fun close() {
        bindChannelFuture.channel().close()
        sessionChannels.depose()
    }
}