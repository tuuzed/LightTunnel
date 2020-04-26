@file:Suppress("CanBeParameter")

package lighttunnel.server

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpVersion
import io.netty.handler.ssl.SslContext
import lighttunnel.dashboard.server.DashboardServer
import lighttunnel.logger.loggerDelegate
import lighttunnel.proto.HeartbeatHandler
import lighttunnel.proto.ProtoMessageDecoder
import lighttunnel.proto.ProtoMessageEncoder
import lighttunnel.server.http.HttpPlugin
import lighttunnel.server.http.HttpRegistry
import lighttunnel.server.http.HttpServer
import lighttunnel.server.interceptor.HttpRequestInterceptor
import lighttunnel.server.interceptor.SimpleRequestInterceptor
import lighttunnel.server.interceptor.TunnelRequestInterceptor
import lighttunnel.server.tcp.TcpRegistry
import lighttunnel.server.tcp.TcpServer
import lighttunnel.util.IncIds
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class TunnelServer(
    private val bossThreads: Int = -1,
    private val workerThreads: Int = -1,
    // tunnel
    private val bindAddr: String? = null,
    private val bindPort: Int = 5080,
    // ssl tunnel
    private val sslBindPort: Int? = null,
    private val sslContext: SslContext? = null,
    private val tunnelRequestInterceptor: TunnelRequestInterceptor = SimpleRequestInterceptor.defaultImpl,
    // http
    private val httpBindPort: Int? = null,
    private val httpRequestInterceptor: HttpRequestInterceptor = SimpleRequestInterceptor.defaultImpl,
    // https
    private val httpsBindPort: Int? = null,
    private val httpsContext: SslContext? = null,
    private val httpsRequestInterceptor: HttpRequestInterceptor = SimpleRequestInterceptor.defaultImpl,
    // plugin
    private val httpPlugin: HttpPlugin? = null,
    // dashboard
    private val dashboardBindPort: Int? = null
) {
    companion object {
        private val EMPTY_JSON_ARRAY = JSONArray(emptyList<Any>())
    }

    private val logger by loggerDelegate()

    private val lock = ReentrantLock()

    private val tunnelIds = IncIds()

    private val bossGroup by lazy { if (bossThreads >= 0) NioEventLoopGroup(bossThreads) else NioEventLoopGroup() }
    private val workerGroup by lazy { if (workerThreads >= 0) NioEventLoopGroup(workerThreads) else NioEventLoopGroup() }

    private var tcpServer: TcpServer? = null
    private var tcpRegistry: TcpRegistry? = null

    private var httpServer: HttpServer? = null
    private var httpRegistry: HttpRegistry? = null

    private var httpsServer: HttpServer? = null
    private var httpsRegistry: HttpRegistry? = null

    private var dashboardServer: DashboardServer? = null

    init {
        if (sslBindPort != null) {
            requireNotNull(sslContext) { "sslContext == null" }
        }
        tcpServer = TcpServer(
            bossGroup = bossGroup,
            workerGroup = workerGroup,
            registry = TcpRegistry().also { tcpRegistry = it }
        )
        if (httpBindPort != null) {
            httpServer = HttpServer(
                bossGroup = bossGroup,
                workerGroup = workerGroup,
                bindAddr = bindAddr,
                bindPort = httpBindPort,
                sslContext = null,
                interceptor = httpRequestInterceptor,
                httpPlugin = httpPlugin,
                registry = HttpRegistry().also { httpRegistry = it }
            )
        }
        if (httpsBindPort != null) {
            requireNotNull(httpsContext) { "httpsContext == null" }
            httpsServer = HttpServer(
                bossGroup = bossGroup,
                workerGroup = workerGroup,
                bindAddr = bindAddr,
                bindPort = httpsBindPort,
                sslContext = httpsContext,
                interceptor = httpsRequestInterceptor,
                httpPlugin = httpPlugin,
                registry = HttpRegistry().also { httpsRegistry = it }
            )
        }
        if (dashboardBindPort != null) {
            val server = DashboardServer(
                bossGroup = bossGroup,
                workerGroup = workerGroup,
                bindAddr = bindAddr,
                bindPort = dashboardBindPort
            ).also { dashboardServer = it }
            server.router {
                route("/api/snapshot") {
                    val obj = JSONObject()
                    obj.put("tcp", tcpRegistry?.snapshot ?: EMPTY_JSON_ARRAY)
                    obj.put("http", httpRegistry?.snapshot ?: EMPTY_JSON_ARRAY)
                    obj.put("https", httpsRegistry?.snapshot ?: EMPTY_JSON_ARRAY)
                    DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1,
                        HttpResponseStatus.OK,
                        Unpooled.copiedBuffer(obj.toString(2), Charsets.UTF_8)
                    )
                }
            }
        }
    }

    @Throws(Exception::class)
    fun start(): Unit = lock.withLock {
        startService(null)
        sslContext?.also { startService(it) }
        httpServer?.start()
        httpsServer?.start()
        dashboardServer?.start()
    }

    fun depose(): Unit = lock.withLock {
        tcpRegistry?.depose()
        httpRegistry?.depose()
        httpsRegistry?.depose()
        dashboardServer?.depose()
        bossGroup.shutdownGracefully()
        workerGroup.shutdownGracefully()
    }

    private fun startService(sslContext: SslContext?) {
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
                            tunnelRequestInterceptor,
                            tunnelIds,
                            tcpServer, tcpRegistry,
                            httpRegistry,
                            httpsRegistry
                        ))
                }
            })
        if (sslContext == null) {
            if (bindAddr == null) {
                serverBootstrap.bind(bindPort).get()
            } else {
                serverBootstrap.bind(bindAddr, bindPort).get()
            }
            logger.info("Serving tunnel on {} port {}", bindAddr ?: "any address", bindPort)
        } else if (sslBindPort != null) {
            if (bindAddr == null) {
                serverBootstrap.bind(sslBindPort).get()
            } else {
                serverBootstrap.bind(bindAddr, sslBindPort).get()
            }
            logger.info("Serving tunnel with ssl on {} port {}", bindAddr ?: "any address", sslBindPort)
        }
    }

}