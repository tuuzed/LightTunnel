package lighttunnel.server.tcp

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import lighttunnel.server.SessionChannels

class TcpServer(
    bossGroup: NioEventLoopGroup,
    workerGroup: NioEventLoopGroup
) {
    val registry = TcpRegistry()

    private val serverBootstrap = ServerBootstrap()

    init {
        this.serverBootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childOption(ChannelOption.AUTO_READ, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel?) {
                    ch ?: return
                    ch.pipeline()
                        .addLast("handler", TcpServerChannelHandler(registry))
                }
            })
    }

    @Throws(Exception::class)
    fun startTunnel(addr: String?, port: Int, sessionChannels: SessionChannels) {
        val bindChannelFuture = if (addr != null) serverBootstrap.bind(addr, port)
        else serverBootstrap.bind(port)
        registry.register(port, sessionChannels, TcpDescriptor(addr, port, sessionChannels, bindChannelFuture))
    }

    fun destroy() = registry.destroy()

}