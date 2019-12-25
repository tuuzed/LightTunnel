package lighttunnel.server.tcp

import io.netty.channel.ChannelFuture
import lighttunnel.server.SessionChannels

class TcpDescriptor(
    val addr: String?,
    val port: Int,
    val sessionChannels: SessionChannels,
    private val bindChannelFuture: ChannelFuture
) {

    val tunnelId get() = sessionChannels.tunnelId
    
    fun close() {
        bindChannelFuture.channel().close()
        sessionChannels.destroy()
    }
}