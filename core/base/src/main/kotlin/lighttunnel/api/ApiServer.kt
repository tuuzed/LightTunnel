package lighttunnel.api

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslHandler
import lighttunnel.logger.loggerDelegate


class ApiServer(
    bossGroup: NioEventLoopGroup,
    workerGroup: NioEventLoopGroup,
    private val bindAddr: String?,
    private val bindPort: Int,
    private val requestDispatcher: RequestDispatcher,
    private val sslContext: SslContext? = null,
    private val maxContentLength: Int = 512 * 1024
) {
    private val logger by loggerDelegate()
    private val serverBootstrap = ServerBootstrap()
    private val isHttps: Boolean get() = sslContext != null
    private var bindChannelFuture: ChannelFuture? = null

    init {
        serverBootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel?) {
                    ch ?: return
                    if (sslContext != null) {
                        ch.pipeline().addFirst(
                            "ssl", SslHandler(sslContext.newEngine(ch.alloc()))
                        )
                    }
                    ch.pipeline()
                        .addLast("codec", HttpServerCodec())
                        .addLast("httpAggregator", HttpObjectAggregator(maxContentLength))
                        .addLast("handler", ApiServerChannelHandler(requestDispatcher))
                }
            })

    }

    fun start() {
        bindChannelFuture = if (bindAddr == null || "0.0.0.0" == bindAddr) {
            serverBootstrap.bind(bindPort)
        } else {
            serverBootstrap.bind(bindAddr, bindPort)
        }
        logger.info(
            "Serving {} on {} port {}",
            if (isHttps) "https" else "http",
            bindAddr ?: "any address",
            bindPort
        )
    }

    fun destroy() {
        bindChannelFuture?.channel()?.close()
    }

    interface RequestDispatcher {
        fun doRequest(request: FullHttpRequest): FullHttpResponse
    }

}