@file:Suppress("CanBeParameter")

package lighttunnel.server

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.*
import io.netty.handler.ssl.SslContext
import lighttunnel.api.server.ApiServer
import lighttunnel.logger.loggerDelegate
import lighttunnel.proto.HeartbeatHandler
import lighttunnel.proto.ProtoMessageDecoder
import lighttunnel.proto.ProtoMessageEncoder
import lighttunnel.server.http.HttpPlugin
import lighttunnel.server.http.HttpRegistry
import lighttunnel.server.http.HttpRequestInterceptor
import lighttunnel.server.http.HttpServer
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
    private val tunnelRequestInterceptor: TunnelRequestInterceptor = TunnelRequestInterceptor.emptyImpl,
    // http
    private val httpBindPort: Int? = null,
    private val httpRequestInterceptor: HttpRequestInterceptor = HttpRequestInterceptor.defaultImpl,
    // https
    private val httpsBindPort: Int? = null,
    private val httpsContext: SslContext? = null,
    private val httpsRequestInterceptor: HttpRequestInterceptor = HttpRequestInterceptor.defaultImpl,
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

    private val bossGroup = if (bossThreads >= 0) NioEventLoopGroup(bossThreads) else NioEventLoopGroup()
    private val workerGroup = if (workerThreads >= 0) NioEventLoopGroup(workerThreads) else NioEventLoopGroup()

    private var tcpServer: TcpServer? = null
    private var tcpRegistry: TcpRegistry? = null

    private var httpServer: HttpServer? = null
    private var httpRegistry: HttpRegistry? = null

    private var httpsServer: HttpServer? = null
    private var httpsRegistry: HttpRegistry? = null

    private var dashboardServer: ApiServer? = null

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
            initHttpServer(httpBindPort)
        }
        if (httpsBindPort != null) {
            requireNotNull(httpsContext) { "httpsContext == null" }
            initHttpsServer(httpsBindPort, httpsContext)
        }
        if (dashboardBindPort != null) {
            initDashboardServer(dashboardBindPort)
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

    private fun initHttpServer(bindPort: Int) {
        httpServer = HttpServer(
            bossGroup = bossGroup,
            workerGroup = workerGroup,
            bindAddr = bindAddr,
            bindPort = bindPort,
            sslContext = null,
            interceptor = httpRequestInterceptor,
            httpPlugin = httpPlugin,
            registry = HttpRegistry().also { httpRegistry = it }
        )
    }

    private fun initHttpsServer(bindPort: Int, sslContext: SslContext) {
        httpsServer = HttpServer(
            bossGroup = bossGroup,
            workerGroup = workerGroup,
            bindAddr = bindAddr,
            bindPort = bindPort,
            sslContext = sslContext,
            interceptor = httpsRequestInterceptor,
            httpPlugin = httpPlugin,
            registry = HttpRegistry().also { httpsRegistry = it }
        )
    }

    private fun initDashboardServer(bindPort: Int) {
        val server = ApiServer(
            bossGroup = bossGroup,
            workerGroup = workerGroup,
            bindAddr = bindAddr,
            bindPort = bindPort
        ).also { dashboardServer = it }
        server.router {
            route("/api/snapshot") {
                val obj = JSONObject()
                obj.put("tcp", tcpRegistry?.snapshot ?: EMPTY_JSON_ARRAY)
                obj.put("http", httpRegistry?.snapshot ?: EMPTY_JSON_ARRAY)
                obj.put("https", httpsRegistry?.snapshot ?: EMPTY_JSON_ARRAY)
                val content = Unpooled.copiedBuffer(obj.toString(2), Charsets.UTF_8)
                DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK,
                    content
                ).also {
                    it.headers()
                        .set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                        .set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes())
                }
            }
        }
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
                            tunnelRequestInterceptor = tunnelRequestInterceptor,
                            tunnelIds = tunnelIds,
                            tcpServer = tcpServer,
                            tcpRegistry = tcpRegistry,
                            httpRegistry = httpRegistry,
                            httpsRegistry = httpsRegistry
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