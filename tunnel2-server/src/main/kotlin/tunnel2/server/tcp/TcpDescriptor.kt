package tunnel2.server.tcp

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelFuture
import tunnel2.server.internal.ServerSessionChannels

class TcpDescriptor(
    val addr: String?,
    val port: Int,
    val sessionChannels: ServerSessionChannels
) {
    private var bindChannelFuture: ChannelFuture? = null

    @Throws(Exception::class)
    fun open(serverBootstrap: ServerBootstrap) {
        bindChannelFuture = if (addr != null) serverBootstrap.bind(addr, port)
        else serverBootstrap.bind(port)
    }

    fun close() {
        sessionChannels.destroy()
    }
}