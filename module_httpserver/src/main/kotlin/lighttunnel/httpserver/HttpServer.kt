package lighttunnel.httpserver

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslHandler
import lighttunnel.common.extensions.injectLogger


class HttpServer(
    bossGroup: NioEventLoopGroup,
    workerGroup: NioEventLoopGroup,
    private val name: String,
    private val bindIp: String?,
    private val bindPort: Int,
    private val sslContext: SslContext? = null,
    private val maxContentLength: Int = 1024 * 1024 * 8,
    routeConfiguration: Routes.() -> Unit
) {
    private val logger by injectLogger()
    private val serverBootstrap = ServerBootstrap()
    private val isHttps: Boolean get() = sslContext != null
    private val routes = Routes()
    private var bindChannelFuture: ChannelFuture? = null

    init {
        serverBootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    if (sslContext != null) {
                        ch.pipeline()
                            .addFirst("ssl", SslHandler(sslContext.newEngine(ch.alloc())))
                    }
                    ch.pipeline()
                        .addLast("codec", HttpServerCodec())
                        .addLast("httpAggregator", HttpObjectAggregator(maxContentLength))
                        .addLast("handler", HttpServerChannelHandler { this@HttpServer.routes.doHandle(it) })
                }
            })
        this.routes.routeConfiguration()
    }

    fun start() {
        bindChannelFuture = if (bindIp == null) {
            serverBootstrap.bind(bindPort)
        } else {
            serverBootstrap.bind(bindIp, bindPort)
        }
        logger.info(
            "Serving {} with {} on {} port {}",
            name,
            if (isHttps) "https" else "http",
            bindIp ?: "::",
            bindPort
        )
    }

    fun depose() {
        bindChannelFuture?.channel()?.close()
    }

}
