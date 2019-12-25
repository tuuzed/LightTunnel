@file:Suppress("CanBeParameter")

package lighttunnel.server

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.ssl.SslContext
import lighttunnel.logger.loggerDelegate
import lighttunnel.proto.HeartbeatHandler
import lighttunnel.proto.ProtoMessageDecoder
import lighttunnel.proto.ProtoMessageEncoder
import lighttunnel.server.http.HttpServer
import lighttunnel.server.interceptor.HttpRequestInterceptor
import lighttunnel.server.interceptor.SimpleRequestInterceptor
import lighttunnel.server.interceptor.TunnelRequestInterceptor
import lighttunnel.server.tcp.TcpServer
import lighttunnel.server.util.IncIds

class TunnelServer(
    private val bossThreads: Int = -1,
    private val workerThreads: Int = -1,
    // tcp
    private val bindAddr: String? = null,
    private val bindPort: Int = 5080,
    private val tunnelRequestInterceptor: TunnelRequestInterceptor = SimpleRequestInterceptor.defaultImpl,
    // ssl
    private val sslBindPort: Int? = null,
    private val sslContext: SslContext? = null,
    // http
    private val httpBindPort: Int? = null,
    private val httpRequestInterceptor: HttpRequestInterceptor = SimpleRequestInterceptor.defaultImpl,
    // https
    private val httpsBindPort: Int? = null,
    private val httpsContext: SslContext? = null,
    private val httpsRequestInterceptor: HttpRequestInterceptor = SimpleRequestInterceptor.defaultImpl
) {
    private val logger by loggerDelegate()
    private val tunnelIds = IncIds()
    private val bossGroup = if (bossThreads >= 0) NioEventLoopGroup(bossThreads) else NioEventLoopGroup()
    private val workerGroup = if (workerThreads >= 0) NioEventLoopGroup(workerThreads) else NioEventLoopGroup()
    private var tcpServer: TcpServer? = null
    private var httpServer: HttpServer? = null
    private var httpsServer: HttpServer? = null

    init {
        if (sslBindPort != null) {
            requireNotNull(sslContext) { "sslContext == null" }
        }
        if (httpBindPort != null) {
            httpServer = HttpServer(
                bossGroup, workerGroup, bindAddr, httpBindPort, null, httpRequestInterceptor
            )
        }
        if (httpsBindPort != null) {
            requireNotNull(httpsContext) { "httpsContext == null" }
            httpsServer = HttpServer(
                bossGroup, workerGroup, bindAddr, httpsBindPort, httpsContext, httpsRequestInterceptor
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
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel?) {
                    ch ?: return
                    if (sslContext != null) {
                        ch.pipeline()
                            .addFirst("ssl", sslContext.newHandler(ch.alloc()))
                    }
                    ch.pipeline()
                        .addLast("heartbeat", HeartbeatHandler())
                        .addLast("decoder", ProtoMessageDecoder())
                        .addLast("encoder", ProtoMessageEncoder())
                        .addLast("handler", TunnelServerChannelHandler(
                            tunnelRequestInterceptor, tunnelIds, tcpServer, httpServer, httpsServer
                        ))
                }
            })
        if (sslContext == null) {
            if (bindAddr == null) serverBootstrap.bind(bindPort).get()
            else serverBootstrap.bind(bindAddr, bindPort).get()
            logger.info("Serving tunnel on {} port {}", bindAddr ?: "any address", bindPort)
        } else if (sslBindPort != null) {
            if (bindAddr == null) serverBootstrap.bind(sslBindPort).get()
            else serverBootstrap.bind(bindAddr, sslBindPort).get()
            logger.info("Serving tunnel with ssl on {} port {}", bindAddr ?: "any address", sslBindPort)
        }
    }

}