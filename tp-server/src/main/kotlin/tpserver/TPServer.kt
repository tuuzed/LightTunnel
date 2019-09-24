package tpserver

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslHandler
import tpcommon.TPHeartbeatHandler
import tpcommon.TPMassageDecoder
import tpcommon.TPMassageEncoder
import tpcommon.logger

class TPServer(
    options: Options = Options()
) {
    private val logger by logger()
    private val options = options.copy()
    private val tunnelIds = TPIds()
    private val bossGroup: NioEventLoopGroup
    private val workerGroup: NioEventLoopGroup
    private var tcpServer: TPTcpServer? = null
    private var httpServer: TPHttpServer? = null
    private var httpsServer: TPHttpServer? = null

    init {
        with(this.options) {
            bossGroup = if (bossThreads >= 0) NioEventLoopGroup(bossThreads) else NioEventLoopGroup()
            workerGroup = if (workerThreads >= 0) NioEventLoopGroup(workerThreads) else NioEventLoopGroup()
            if (sslEnable) {
                requireNotNull(sslContext) { "sslContext == null" }
            }
            if (tcpEnable) {
                tcpServer = TPTcpServer(bossGroup, workerGroup)
            }
            if (httpEnable) {
                httpServer = TPHttpServer(
                    bossGroup, workerGroup, null, httpBindAddr, httpBindPort, tpHttpRequestInterceptor
                )
            }
            if (httpsEnable) {
                requireNotNull(options.httpsContext) { "httpsContext == null" }
                httpsServer = TPHttpServer(
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
                .addLast(TPHeartbeatHandler())
                .addLast(TPMassageDecoder())
                .addLast(TPMassageEncoder())
                .addLast(
                    TPServerChannelHandler(
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
        var tpRequestInterceptor: TPRequestInterceptor = TPRequestInterceptor.EMPTY_IMPL,
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
        var tpHttpRequestInterceptor: TPHttpRequestInterceptor = TPHttpRequestInterceptor.EMPTY_IMPL,
        // https
        var httpsEnable: Boolean = false,
        var httpsContext: SslContext? = null,
        var httpsBindAddr: String? = null,
        var httpsBindPort: Int = 443,
        var tpHttpsRequestInterceptor: TPHttpRequestInterceptor = TPHttpRequestInterceptor.EMPTY_IMPL
    )

}