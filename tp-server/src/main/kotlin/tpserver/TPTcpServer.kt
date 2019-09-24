package tpserver

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel

class TPTcpServer(
    bossGroup: NioEventLoopGroup,
    workerGroup: NioEventLoopGroup
) {
    val registry = TPTcpRegistry()
    private val serverBootstrap = ServerBootstrap()

    init {
        this.serverBootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childOption(ChannelOption.AUTO_READ, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(createChannelInitializer())
    }

    @Throws(Exception::class)
    fun startTunnel(addr: String?, port: Int, sessionPool: TPSessionPool) {
        val descriptor = TPTcpDescriptor(addr, port, sessionPool)
        descriptor.open(serverBootstrap)
        registry.register(port, sessionPool, descriptor)
    }

    fun destroy() = registry.destroy()

    private fun createChannelInitializer() = object : ChannelInitializer<SocketChannel>() {
        override fun initChannel(ch: SocketChannel?) {
            ch ?: return
            ch.pipeline().addLast(TPTcpServerChannelHandler(registry))
        }
    }

}