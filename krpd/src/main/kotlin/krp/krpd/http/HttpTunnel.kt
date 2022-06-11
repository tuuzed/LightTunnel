package krp.krpd.http

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.HttpRequestDecoder
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslHandler
import krp.common.exception.KrpException
import krp.common.utils.injectLogger
import krp.krpd.SessionChannels


internal class HttpTunnel(
    bossGroup: NioEventLoopGroup,
    workerGroup: NioEventLoopGroup,
    private val registry: HttpRegistry,
    private val bindIp: String?,
    private val bindPort: Int,
    private val sslContext: SslContext? = null,
    private val httpPlugin: HttpPlugin? = null,
    private val httpTunnelRequestInterceptor: HttpTunnelRequestInterceptor? = null
) {
    private val logger by injectLogger()
    private val serverBootstrap = ServerBootstrap()
    private val isHttps: Boolean get() = sslContext != null

    init {
        this.serverBootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childOption(ChannelOption.AUTO_READ, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    if (sslContext != null) {
                        ch.pipeline().addFirst("ssl", SslHandler(sslContext.newEngine(ch.alloc())))
                    }
                    ch.pipeline()
                        .addLast("decoder", HttpRequestDecoder())
                        .addLast(
                            "handler", HttpTunnelChannelHandler(
                                registry = registry,
                                httpPlugin = httpPlugin,
                                httpTunnelRequestInterceptor = httpTunnelRequestInterceptor
                            )
                        )
                }
            })
    }

    fun start() {
        if (bindIp == null) {
            serverBootstrap.bind(bindPort).get()
        } else {
            serverBootstrap.bind(bindIp, bindPort).get()
        }
        logger.info(
            "Serving tunnel by {} on {} port {}",
            if (isHttps) "https" else "http",
            bindIp ?: "::",
            bindPort
        )
    }

    fun stopTunnel(host: String) = registry.unregister(host)

    @Throws(Exception::class)
    fun requireUnregistered(host: String) {
        if (registry.isRegistered(host)) {
            throw KrpException("host($host) already used")
        }
    }

    @Throws(Exception::class)
    fun startTunnel(host: String, sessionChannels: SessionChannels): DefaultHttpFd {
        requireUnregistered(host)
        return registry.register(isHttps, host, sessionChannels)
    }

}
