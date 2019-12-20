@file:Suppress("CanBeParameter")

package lighttunnel.server

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslHandler
import lighttunnel.logging.loggerDelegate
import lighttunnel.proto.HeartbeatHandler
import lighttunnel.proto.ProtoMassageDecoder
import lighttunnel.proto.ProtoMassageEncoder
import lighttunnel.server.http.HttpRequestInterceptor
import lighttunnel.server.http.HttpServer
import lighttunnel.server.interceptor.RequestInterceptor
import lighttunnel.server.tcp.TcpServer
import lighttunnel.server.util.IncId

class TunnelServer(
    private val bossThreads: Int = -1,
    private val workerThreads: Int = -1,
    // tcp
    private val bindAddr: String? = null,
    private val bindPort: Int = 5080,
    private val requestInterceptor: RequestInterceptor = RequestInterceptor.EMPTY_IMPL,
    // ssl
    private val sslBindPort: Int? = null,
    private val sslContext: SslContext? = null,
    // http
    private val httpBindPort: Int? = null,
    private val httpRequestInterceptor: HttpRequestInterceptor = HttpRequestInterceptor.EMPTY_IMPL,
    // https
    private val httpsBindPort: Int? = null,
    private val httpsContext: SslContext? = null,
    private val httpsRequestInterceptor: HttpRequestInterceptor = HttpRequestInterceptor.EMPTY_IMPL
) {
    private val logger by loggerDelegate()
    private val tunnelIds = IncId()
    private val bossGroup: NioEventLoopGroup =
        if (bossThreads >= 0) NioEventLoopGroup(bossThreads) else NioEventLoopGroup()
    private val workerGroup: NioEventLoopGroup =
        if (workerThreads >= 0) NioEventLoopGroup(workerThreads) else NioEventLoopGroup()
    private var tcpServer: TcpServer? = null
    private var httpServer: HttpServer? = null
    private var httpsServer: HttpServer? = null

    init {
        if (sslBindPort != null) {
            requireNotNull(sslContext) { "sslContext == null" }
        }
        if (httpBindPort != null) {
            httpServer = HttpServer(
                bossGroup, workerGroup, null, bindAddr, httpBindPort, httpRequestInterceptor
            )
        }
        if (httpsBindPort != null) {
            requireNotNull(httpsContext) { "httpsContext == null" }
            httpsServer = HttpServer(
                bossGroup, workerGroup, httpsContext, bindAddr, httpsBindPort, httpsRequestInterceptor
            )
        }
        tcpServer = TcpServer(bossGroup, workerGroup)
    }

    @Synchronized
    @Throws(Exception::class)
    fun start() {
        startTunnelService(null)
        sslContext?.also { startTunnelService(it) }
        httpServer?.start()
        httpsServer?.start()
    }


    @Synchronized
    fun destroy() {
        tcpServer?.destroy()
        httpServer?.destroy()
        httpsServer?.destroy()
        bossGroup.shutdownGracefully()
        workerGroup.shutdownGracefully()
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
        } else if (sslBindPort != null) {
            if (bindAddr == null) serverBootstrap.bind(sslBindPort).get()
            else serverBootstrap.bind(bindAddr, sslBindPort).get()
            logger.info("Serving ssl tunnel on {} port {}", bindAddr ?: "any address", sslBindPort)
        }
    }

    private fun createChannelInitializer(sslContext: SslContext?) = object : ChannelInitializer<SocketChannel>() {
        override fun initChannel(ch: SocketChannel?) {
            ch ?: return
            if (sslContext != null) ch.pipeline().addFirst(SslHandler(sslContext.newEngine(ch.alloc())))
            ch.pipeline()
                .addLast(HeartbeatHandler())
                .addLast(ProtoMassageDecoder())
                .addLast(ProtoMassageEncoder())
                .addLast(
                    TunnelServerChannelHandler(requestInterceptor, tunnelIds, tcpServer, httpServer, httpsServer)
                )
        }
    }

}