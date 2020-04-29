package lighttunnel.server.http

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpRequestDecoder
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslHandler
import lighttunnel.logger.loggerDelegate


class HttpServer(
    bossGroup: NioEventLoopGroup,
    workerGroup: NioEventLoopGroup,
    private val registry: HttpRegistry,
    private val bindAddr: String?,
    private val bindPort: Int,
    private val sslContext: SslContext? = null,
    private val maxContentLength: Int = 1024 * 1024 * 8,
    private val httpPlugin: HttpPlugin? = null,
    private val interceptor: HttpRequestInterceptor
) {
    private val logger by loggerDelegate()
    private val serverBootstrap = ServerBootstrap()
    private val isHttps: Boolean get() = sslContext != null

    init {
        this.serverBootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childOption(ChannelOption.AUTO_READ, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel?) {
                    ch ?: return
                    if (sslContext != null) {
                        ch.pipeline().addFirst(
                            "ssl", SslHandler(sslContext.newEngine(ch.alloc()))
                        )
                    }
                    ch.pipeline()
                        .addLast("decoder", HttpRequestDecoder())
                        .addLast("httpAggregator", HttpObjectAggregator(maxContentLength))
                        .addLast("handler", HttpServerChannelHandler(
                            registry = registry,
                            interceptor = interceptor,
                            httpPlugin = httpPlugin
                        ))
                }
            })
    }

    fun start() {
        if (bindAddr == null) {
            serverBootstrap.bind(bindPort).get()
        } else {
            serverBootstrap.bind(bindAddr, bindPort).get()
        }
        logger.info(
            "Serving {} on {} port {}",
            if (isHttps) "https" else "http",
            bindAddr ?: "any address",
            bindPort
        )
    }

}