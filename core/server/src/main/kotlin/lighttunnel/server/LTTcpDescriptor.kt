package lighttunnel.server

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelFuture

class LTTcpDescriptor(
    val addr: String?,
    val port: Int,
    val sessionPool: LTSessionPool
) {
    private var bindChannelFuture: ChannelFuture? = null

    @Throws(Exception::class)
    fun open(serverBootstrap: ServerBootstrap) {
        bindChannelFuture = if (addr != null) serverBootstrap.bind(addr, port)
        else serverBootstrap.bind(port)
    }

    fun close() {
        bindChannelFuture?.channel()?.close()
        sessionPool.destroy()
    }
}