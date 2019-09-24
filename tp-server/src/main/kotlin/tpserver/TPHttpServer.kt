package tpserver

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.HttpRequestDecoder
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslHandler
import tpcommon.logger


class TPHttpServer(
    bossGroup: NioEventLoopGroup,
    workerGroup: NioEventLoopGroup,
    private val sslContext: SslContext? = null,
    private val bindAddr: String?,
    private val bindPort: Int,
    private val interceptor: TPHttpRequestInterceptor
) {
    private val logger by logger()

    val registry = TPHttpRegistry()
    private val serverBootstrap = ServerBootstrap()
    private val https: Boolean get() = sslContext != null

    init {
        this.serverBootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childOption(ChannelOption.AUTO_READ, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(createChannelInitializer(sslContext))
    }

    fun start() {
        if (bindAddr != null) {
            serverBootstrap.bind(bindAddr, bindPort).get()
        } else {
            serverBootstrap.bind(bindPort).get()
        }
        logger.info(
            "Serving {} on {} port {}",
            if (https) "https" else "http",
            bindAddr ?: "any address",
            bindPort
        )
    }

    fun shutdown() = registry.destroy()
    fun destroy() = shutdown()

    private fun createChannelInitializer(sslContext: SslContext?) = object : ChannelInitializer<SocketChannel>() {
        override fun initChannel(ch: SocketChannel?) {
            ch ?: return
            if (sslContext != null) {
                ch.pipeline().addFirst(
                    SslHandler(sslContext.newEngine(ch.alloc()))
                )
            }
            ch.pipeline()
                .addLast(HttpRequestDecoder())
                .addLast(TPHttpServerChannelHandler(registry, interceptor))
        }
    }
}