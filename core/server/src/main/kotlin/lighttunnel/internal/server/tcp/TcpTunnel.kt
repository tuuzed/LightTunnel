package lighttunnel.internal.server.tcp

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import lighttunnel.ProtoException
import lighttunnel.internal.base.util.PortUtil
import lighttunnel.internal.server.util.SessionChannels

internal class TcpTunnel(
    bossGroup: NioEventLoopGroup,
    workerGroup: NioEventLoopGroup,
    private val registry: TcpRegistry
) {

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
                        .addLast("handler", TcpTunnelChannelHandler(registry))
                }
            })
    }

    @Throws(Exception::class)
    fun requireNotRegistered(port: Int) {
        if (registry.isRegistered(port) || !PortUtil.isAvailablePort(port)) {
            throw ProtoException("port($port) already used")
        }
    }

    fun stopTunnel(port: Int) = registry.unregister(port)

    @Throws(Exception::class)
    fun startTunnel(addr: String?, port: Int, sessionChannels: SessionChannels): TcpFdDefaultImpl {
        requireNotRegistered(port)
        val bindChannelFuture = if (addr == null) {
            serverBootstrap.bind(port)
        } else {
            serverBootstrap.bind(addr, port)
        }
        val fd = TcpFdDefaultImpl(sessionChannels) { bindChannelFuture.channel().close() }
        registry.register(port, fd)
        return fd
    }

}