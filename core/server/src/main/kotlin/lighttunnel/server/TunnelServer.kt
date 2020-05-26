@file:Suppress("CanBeParameter")

package lighttunnel.server

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.*
import io.netty.handler.ssl.SslContext
import lighttunnel.logger.loggerDelegate
import lighttunnel.proto.HeartbeatHandler
import lighttunnel.proto.ProtoMessageDecoder
import lighttunnel.proto.ProtoMessageEncoder
import lighttunnel.server.http.*
import lighttunnel.server.tcp.TcpFd
import lighttunnel.server.tcp.TcpRegistry
import lighttunnel.server.tcp.TcpServer
import lighttunnel.util.IncIds
import lighttunnel.web.server.WebServer
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class TunnelServer(
    private val bossThreads: Int = -1,
    private val workerThreads: Int = -1,
    // tunnel
    private val tunnelServiceArgs: TunnelServiceArgs = TunnelServiceArgs(),
    private val sslTunnelServiceArgs: SslTunnelServiceArgs? = null,
    private val httpServerArgs: HttpServerArgs? = null,
    private val httpsServerArgs: HttpsServerArgs? = null,
    private val webServerArgs: WebServerArgs? = null,
    // listener
    private val onTcpTunnelStateListener: OnTcpTunnelStateListener? = null,
    private val onHttpTunnelStateListener: OnHttpTunnelStateListener? = null
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

    private var webServer: WebServer? = null

    private val onChannelStateListener = OnChannelStateListenerImpl(
        onTcpTunnelStateListener,
        onHttpTunnelStateListener
    )

    @Throws(Exception::class)
    fun start(): Unit = lock.withLock {
        tcpServer = createTcpServer()
        httpServer = createHttpServer()?.apply { start() }
        httpsServer = createHttpsServer()?.apply { start() }
        webServer = createWebServer()?.apply { start() }
        startTunnelService()
        startSslTunnelService()
    }

    fun depose(): Unit = lock.withLock {
        tcpRegistry?.depose()
        httpRegistry?.depose()
        httpsRegistry?.depose()
        webServer?.depose()
        bossGroup.shutdownGracefully()
        workerGroup.shutdownGracefully()
    }

    private fun startTunnelService() {
        val args = tunnelServiceArgs
        val serverBootstrap = ServerBootstrap()
        serverBootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childOption(ChannelOption.AUTO_READ, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel?) {
                    ch ?: return
                    ch.pipeline()
                        .addLast("heartbeat", HeartbeatHandler())
                        .addLast("decoder", ProtoMessageDecoder())
                        .addLast("encoder", ProtoMessageEncoder())
                        .addLast("handler", TunnelServerChannelHandler(
                            tunnelRequestInterceptor = args.tunnelRequestInterceptor,
                            tunnelIds = tunnelIds,
                            tcpServer = tcpServer,
                            httpServer = httpServer,
                            httpsServer = httpsServer,
                            onChannelStateListener = onChannelStateListener
                        ))
                }
            })
        if (args.bindAddr == null) {
            serverBootstrap.bind(args.bindPort).get()
        } else {
            serverBootstrap.bind(args.bindAddr, args.bindPort).get()
        }
        logger.info("Serving tunnel on {} port {}", args.bindAddr ?: "::", args.bindPort)
    }

    private fun startSslTunnelService() {
        val args = sslTunnelServiceArgs ?: return
        if (args.bindPort == null) return
        val serverBootstrap = ServerBootstrap()
        serverBootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childOption(ChannelOption.AUTO_READ, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel?) {
                    ch ?: return
                    ch.pipeline()
                        .addFirst("ssl", args.sslContext.newHandler(ch.alloc()))
                        .addLast("heartbeat", HeartbeatHandler())
                        .addLast("decoder", ProtoMessageDecoder())
                        .addLast("encoder", ProtoMessageEncoder())
                        .addLast("handler", TunnelServerChannelHandler(
                            tunnelRequestInterceptor = args.tunnelRequestInterceptor,
                            tunnelIds = tunnelIds,
                            tcpServer = tcpServer,
                            httpServer = httpServer,
                            httpsServer = httpsServer,
                            onChannelStateListener = onChannelStateListener
                        ))
                }
            })
        if (args.bindAddr == null) {
            serverBootstrap.bind(args.bindPort).get()
        } else {
            serverBootstrap.bind(args.bindAddr, args.bindPort).get()
        }
        logger.info("Serving tunnel with ssl on {} port {}", args.bindAddr ?: "::", args.bindPort)
    }

    private fun createTcpServer(): TcpServer {
        return TcpServer(
            bossGroup = bossGroup,
            workerGroup = workerGroup,
            registry = TcpRegistry().also { tcpRegistry = it }
        )
    }

    private fun createHttpServer(): HttpServer? {
        val args = httpServerArgs ?: return null
        return HttpServer(
            bossGroup = bossGroup,
            workerGroup = workerGroup,
            bindAddr = args.bindAddr,
            bindPort = args.bindPort ?: return null,
            sslContext = null,
            interceptor = args.httpRequestInterceptor,
            httpPlugin = args.httpPlugin,
            registry = HttpRegistry().also { httpRegistry = it }
        )
    }

    private fun createHttpsServer(): HttpServer? {
        val args = httpsServerArgs ?: return null
        if (args.bindPort == null) return null
        return HttpServer(
            bossGroup = bossGroup,
            workerGroup = workerGroup,
            bindAddr = args.bindAddr,
            bindPort = args.bindPort,
            sslContext = args.sslContext,
            interceptor = args.httpRequestInterceptor,
            httpPlugin = args.httpPlugin,
            registry = HttpRegistry().also { httpsRegistry = it }
        )
    }

    private fun createWebServer(): WebServer? {
        val args = webServerArgs ?: return null
        return WebServer(
            bossGroup = bossGroup,
            workerGroup = workerGroup,
            bindAddr = args.bindAddr,
            bindPort = args.bindPort ?: return null
        ).apply {
            webServer = this
            router {
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
    }

    class TunnelServiceArgs(
        val bindAddr: String? = null,
        val bindPort: Int = 5080,
        val tunnelRequestInterceptor: TunnelRequestInterceptor = TunnelRequestInterceptor.emptyImpl
    )

    class SslTunnelServiceArgs(
        val bindAddr: String? = null,
        val bindPort: Int? = null,
        val tunnelRequestInterceptor: TunnelRequestInterceptor = TunnelRequestInterceptor.emptyImpl,
        val sslContext: SslContext
    )

    class HttpServerArgs(
        val bindAddr: String? = null,
        val bindPort: Int? = null,
        val httpRequestInterceptor: HttpRequestInterceptor = HttpRequestInterceptor.defaultImpl,
        val httpPlugin: HttpPlugin? = null
    )

    class HttpsServerArgs(
        val bindAddr: String? = null,
        val bindPort: Int? = null,
        val httpRequestInterceptor: HttpRequestInterceptor = HttpRequestInterceptor.defaultImpl,
        val httpPlugin: HttpPlugin? = null,
        val sslContext: SslContext
    )

    class WebServerArgs(
        val bindAddr: String? = null,
        val bindPort: Int? = null
    )

    private class OnChannelStateListenerImpl(
        private val onTcpTunnelStateListener: OnTcpTunnelStateListener?,
        private val onHttpTunnelStateListener: OnHttpTunnelStateListener?
    ) : TunnelServerChannelHandler.OnChannelStateListener {
        override fun onChannelConnected(ctx: ChannelHandlerContext, tcpFd: TcpFd?) {
            super.onChannelConnected(ctx, tcpFd)
            if (tcpFd != null) {
                onTcpTunnelStateListener?.onConnected(tcpFd)
            }
        }

        override fun onChannelInactive(ctx: ChannelHandlerContext, tcpFd: TcpFd?) {
            super.onChannelInactive(ctx, tcpFd)
            if (tcpFd != null) {
                onTcpTunnelStateListener?.onDisconnect(tcpFd)
            }
        }

        override fun onChannelConnected(ctx: ChannelHandlerContext, httpFd: HttpFd?) {
            super.onChannelConnected(ctx, httpFd)
            if (httpFd != null) {
                onHttpTunnelStateListener?.onConnected(httpFd)
            }
        }

        override fun onChannelInactive(ctx: ChannelHandlerContext, httpFd: HttpFd?) {
            super.onChannelInactive(ctx, httpFd)
            if (httpFd != null) {
                onHttpTunnelStateListener?.onDisconnect(httpFd)
            }
        }
    }

    interface OnTcpTunnelStateListener {
        fun onConnected(fd: TcpFd) {}
        fun onDisconnect(fd: TcpFd) {}
    }

    interface OnHttpTunnelStateListener {
        fun onConnected(fd: HttpFd) {}
        fun onDisconnect(fd: HttpFd) {}
    }


}