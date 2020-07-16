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
import lighttunnel.base.util.loggerDelegate
import lighttunnel.openapi.ProtoException
import lighttunnel.openapi.http.HttpPlugin
import lighttunnel.openapi.http.HttpTunnelRequestInterceptor
import lighttunnel.server.util.SessionChannels


internal class HttpTunnel(
    bossGroup: NioEventLoopGroup,
    workerGroup: NioEventLoopGroup,
    private val registry: HttpRegistry,
    private val bindAddr: String?,
    private val bindPort: Int,
    private val sslContext: SslContext? = null,
    private val maxContentLength: Int = 1024 * 1024 * 8,
    private val httpPlugin: HttpPlugin? = null,
    private val httpTunnelRequestInterceptor: HttpTunnelRequestInterceptor? = null
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
                        .addLast("handler", HttpTunnelChannelHandler(
                            registry = registry,
                            httpPlugin = httpPlugin,
                            httpTunnelRequestInterceptor = httpTunnelRequestInterceptor
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

    fun stopTunnel(host: String) = registry.unregister(host)

    @Throws(Exception::class)
    fun requireNotRegistered(host: String) {
        if (registry.isRegistered(host)) {
            throw ProtoException("host($host) already used")
        }
    }

    @Throws(Exception::class)
    fun startTunnel(host: String, sessionChannels: SessionChannels): HttpFdDefaultImpl {
        requireNotRegistered(host)
        return registry.register(isHttps, host, sessionChannels)
    }

}