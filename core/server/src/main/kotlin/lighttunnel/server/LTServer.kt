package lighttunnel.server

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslHandler
import lighttunnel.logging.logger
import lighttunnel.proto.LTHeartbeatHandler
import lighttunnel.proto.LTMassageDecoder
import lighttunnel.proto.LTMassageEncoder

class LTServer(
    options: Options = Options()
) {
    private val logger by logger()
    private val options = options.copy()
    private val tunnelIds = LTIncIds()
    private val bossGroup: NioEventLoopGroup
    private val workerGroup: NioEventLoopGroup
    private var tcpServer: LTTcpServer? = null
    private var httpServer: LTHttpServer? = null
    private var httpsServer: LTHttpServer? = null

    init {
        with(this.options) {
            bossGroup = if (bossThreads >= 0) NioEventLoopGroup(bossThreads) else NioEventLoopGroup()
            workerGroup = if (workerThreads >= 0) NioEventLoopGroup(workerThreads) else NioEventLoopGroup()
            if (sslEnable) {
                requireNotNull(sslContext) { "sslContext == null" }
            }
            if (tcpEnable) {
                tcpServer = LTTcpServer(bossGroup, workerGroup)
            }
            if (httpEnable) {
                httpServer = LTHttpServer(
                    bossGroup, workerGroup, null, httpBindAddr, httpBindPort, tpHttpRequestInterceptor
                )
            }
            if (httpsEnable) {
                requireNotNull(options.httpsContext) { "httpsContext == null" }
                httpsServer = LTHttpServer(
                    bossGroup, workerGroup, httpsContext, httpsBindAddr, httpsBindPort, tpHttpsRequestInterceptor
                )
            }
        }
    }

    @Synchronized
    @Throws(Exception::class)
    fun start() {
        startTunnelService(null)
        options.sslContext?.also { startTunnelService(it) }
        httpServer?.start()
        httpsServer?.start()
    }

    private fun startTunnelService(sslContext: SslContext?) {
        val serverBootstrap = ServerBootstrap()
        serverBootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childOption(ChannelOption.AUTO_READ, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(createChannelInitializer(sslContext))
        if (sslContext == null) {
            if (options.bindAddr == null) serverBootstrap.bind(options.bindPort).get()
            else serverBootstrap.bind(options.bindAddr, options.bindPort).get()
            logger.info("Serving tunnel on {} port {}", options.bindAddr ?: "any address", options.bindPort)
        } else {
            if (options.sslBindAddr == null) serverBootstrap.bind(options.sslBindPort).get()
            else serverBootstrap.bind(options.sslBindAddr, options.sslBindPort).get()
            logger.info("Serving ssl tunnel on {} port {}", options.sslBindAddr ?: "any address", options.sslBindPort)
        }
    }

    @Synchronized
    fun destroy() {
        tcpServer?.destroy()
        httpServer?.destroy()
        httpsServer?.destroy()
        bossGroup.shutdownGracefully()
        workerGroup.shutdownGracefully()
    }

    private fun createChannelInitializer(sslContext: SslContext?) = object : ChannelInitializer<SocketChannel>() {
        override fun initChannel(ch: SocketChannel?) {
            ch ?: return
            if (sslContext != null) ch.pipeline().addFirst(SslHandler(sslContext.newEngine(ch.alloc())))
            ch.pipeline()
                .addLast(LTHeartbeatHandler())
                .addLast(LTMassageDecoder())
                .addLast(LTMassageEncoder())
                .addLast(
                    LTServerChannelHandler(
                        options.tpRequestInterceptor,
                        tunnelIds,
                        tcpServer,
                        httpServer,
                        httpsServer
                    )
                )
        }
    }

    data class Options(
        var bossThreads: Int = -1,
        var workerThreads: Int = -1,
        // tunnel
        var tpRequestInterceptor: LTRequestInterceptor = LTRequestInterceptor.EMPTY_IMPL,
        var bindAddr: String? = null,
        var bindPort: Int = 5080,
        var sslEnable: Boolean = false,
        var sslContext: SslContext? = null,
        var sslBindAddr: String? = null,
        var sslBindPort: Int = 5443,
        // tcp
        var tcpEnable: Boolean = true,
        // http
        var httpEnable: Boolean = false,
        var httpBindAddr: String? = null,
        var httpBindPort: Int = 80,
        var tpHttpRequestInterceptor: LTHttpRequestInterceptor = LTHttpRequestInterceptor.EMPTY_IMPL,
        // https
        var httpsEnable: Boolean = false,
        var httpsContext: SslContext? = null,
        var httpsBindAddr: String? = null,
        var httpsBindPort: Int = 443,
        var tpHttpsRequestInterceptor: LTHttpRequestInterceptor = LTHttpRequestInterceptor.EMPTY_IMPL
    )

}