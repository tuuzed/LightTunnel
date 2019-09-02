package tunnel2.server.tcp

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import tunnel2.server.internal.ServerSessionChannels

class TcpServer(
    bossGroup: NioEventLoopGroup,
    workerGroup: NioEventLoopGroup
) {
    private val serverBootstrap = ServerBootstrap()

    val registry = TcpRegistry()

    init {
        this.serverBootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childOption(ChannelOption.AUTO_READ, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(createChannelInitializer())
    }

    fun startTunnel(addr: String?, port: Int, sessionChannels: ServerSessionChannels) {
        val descriptor = TcpDescriptor(addr, port, sessionChannels)
        descriptor.open(serverBootstrap)
        registry.register(port, sessionChannels, descriptor)
    }

    fun destroy() = registry.destroy()

    private fun createChannelInitializer() = object : ChannelInitializer<SocketChannel>() {
        override fun initChannel(ch: SocketChannel?) {
            ch ?: return
            ch.pipeline().addLast(TcpServerChannelHandler(registry))
        }
    }
}