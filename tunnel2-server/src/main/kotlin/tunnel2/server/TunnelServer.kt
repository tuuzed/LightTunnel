package tunnel2.server

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslHandler
import tunnel2.common.TunnelHeartbeatHandler
import tunnel2.common.logging.LoggerFactory
import tunnel2.common.proto.ProtoMessageDecoder
import tunnel2.common.proto.ProtoMessageEncoder
import tunnel2.server.http.HttpServer
import tunnel2.server.interceptor.HttpRequestInterceptor
import tunnel2.server.interceptor.TunnelRequestInterceptor
import tunnel2.server.internal.IdProducer
import tunnel2.server.tcp.TcpServer


class TunnelServer(
    bossThreads: Int = -1,
    workerThreads: Int = -1,
    private val tunnelRequestInterceptor: TunnelRequestInterceptor = TunnelRequestInterceptor.EMPTY_IMPL,
    private val bindAddr: String? = null,
    private val bindPort: Int = 5000,
    // ssl
    sslEnable: Boolean = false,
    private val sslContext: SslContext? = null,
    private val sslBindAddr: String? = null,
    private val sslBindPort: Int = 5001,
    // tcp
    tcpEnable: Boolean = true,
    // http
    httpEnable: Boolean = false,
    httpBindAddr: String? = null,
    httpBindPort: Int = 5080,
    httpRequestInterceptor: HttpRequestInterceptor = HttpRequestInterceptor.EMPTY_IMPL,
    // https
    httpsEnable: Boolean = false,
    httpsContext: SslContext? = null,
    httpsBindAddr: String? = null,
    httpsBindPort: Int = 5443,
    httpsRequestInterceptor: HttpRequestInterceptor = HttpRequestInterceptor.EMPTY_IMPL

) {
    companion object {
        private val logger = LoggerFactory.getLogger(TunnelServer::class.java)
    }

    private val tunnelIdProducer = IdProducer()
    private val bossGroup: NioEventLoopGroup =
        if (bossThreads >= 0) NioEventLoopGroup(bossThreads) else NioEventLoopGroup()
    private val workerGroup: NioEventLoopGroup =
        if (workerThreads >= 0) NioEventLoopGroup(workerThreads) else NioEventLoopGroup()

    private var tcpServer: TcpServer? = null
    private var httpServer: HttpServer? = null
    private var httpsServer: HttpServer? = null

    init {
        if (sslEnable) {
            requireNotNull(sslContext) { "sslContext == null" }
        }
        if (tcpEnable) {
            tcpServer = TcpServer(bossGroup, workerGroup)
        }
        if (httpEnable) {
            httpServer = HttpServer(bossGroup, workerGroup, null, httpBindAddr, httpBindPort, httpRequestInterceptor)
        }
        if (httpsEnable) {
            requireNotNull(httpsContext) { "httpsContext == null" }
            httpsServer = HttpServer(bossGroup, workerGroup, httpsContext, httpsBindAddr, httpsBindPort, httpsRequestInterceptor)
        }

    }

    @Synchronized
    @Throws(Exception::class)
    fun start() {
        startTunnelService(null)
        sslContext?.also { startTunnelService(it) }
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
            if (bindAddr == null) serverBootstrap.bind(bindPort).get()
            else serverBootstrap.bind(bindAddr, bindPort).get()
            logger.info("Serving tunnel on {} port {}", bindAddr ?: "any address", bindPort)
        } else {
            if (sslBindAddr == null) serverBootstrap.bind(sslBindPort).get()
            else serverBootstrap.bind(sslBindAddr, sslBindPort).get()
            logger.info("Serving ssl tunnel on {} port {}", sslBindAddr ?: "any address", sslBindPort)
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
            sslContext?.also {
                ch.pipeline()
                    .addFirst(SslHandler(it.newEngine(ch.alloc())))
            }
            ch.pipeline()
                .addLast(ProtoMessageDecoder())
                .addLast(ProtoMessageEncoder())
                .addLast(TunnelHeartbeatHandler())
                .addLast(
                    TunnelServerChannelHandler(
                        tunnelRequestInterceptor,
                        tunnelIdProducer,
                        tcpServer, httpServer, httpsServer
                    )
                )
        }
    }

}