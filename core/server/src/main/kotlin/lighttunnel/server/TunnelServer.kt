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
import lighttunnel.server.tcp.TcpTunnel
import lighttunnel.server.traffic.TrafficHandler
import lighttunnel.util.IncIds
import lighttunnel.web.server.WebServer
import org.json.JSONObject
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class TunnelServer(
    bossThreads: Int = -1,
    workerThreads: Int = -1,
    private val tunnelDaemonArgs: TunnelDaemonArgs = TunnelDaemonArgs(),
    private val sslTunnelDaemonArgs: SslTunnelDaemonArgs? = null,
    httpTunnelArgs: HttpTunnelArgs? = null,
    httpsTunnelArgs: HttpsTunnelArgs? = null,
    webServerArgs: WebServerArgs? = null,
    onTcpTunnelStateListener: OnTcpTunnelStateListener? = null,
    onHttpTunnelStateListener: OnHttpTunnelStateListener? = null
) {
    private val logger by loggerDelegate()
    private val lock = ReentrantLock()
    private val tunnelIds = IncIds()

    private val bossGroup = if (bossThreads >= 0) NioEventLoopGroup(bossThreads) else NioEventLoopGroup()
    private val workerGroup = if (workerThreads >= 0) NioEventLoopGroup(workerThreads) else NioEventLoopGroup()

    private val tcpRegistry by lazy { TcpRegistry() }
    private val tcpTunnel: TcpTunnel = createTcpTunnel(tcpRegistry)

    private val httpRegistry by lazy { HttpRegistry() }
    private val httpTunnel: HttpTunnel? = httpTunnelArgs?.let { createHttpTunnel(httpRegistry, it) }

    private val httpsRegistry by lazy { HttpRegistry() }
    private val httpsTunnel: HttpTunnel? = httpsTunnelArgs?.let { createHttpsTunnel(httpsRegistry, it) }

    private val webServer: WebServer? = webServerArgs?.let { createWebServer(it) }

    private val onChannelStateListener = OnChannelStateListenerImpl(
        onTcpTunnelStateListener,
        onHttpTunnelStateListener
    )

    @Throws(Exception::class)
    fun start(): Unit = lock.withLock {
        httpTunnel?.start()
        httpsTunnel?.start()
        webServer?.start()
        startTunnelDaemon(tunnelDaemonArgs)
        sslTunnelDaemonArgs?.also { startSslTunnelDaemon(it) }
    }

    fun depose(): Unit = lock.withLock {
        tcpRegistry.depose()
        httpRegistry.depose()
        httpsRegistry.depose()
        webServer?.depose()
        bossGroup.shutdownGracefully()
        workerGroup.shutdownGracefully()
    }

    private fun startTunnelDaemon(args: TunnelDaemonArgs) {
        val serverBootstrap = ServerBootstrap()
        serverBootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childOption(ChannelOption.AUTO_READ, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel?) {
                    ch ?: return
                    ch.pipeline()
                        .addLast("traffic", TrafficHandler())
                        .addLast("heartbeat", HeartbeatHandler())
                        .addLast("decoder", ProtoMessageDecoder())
                        .addLast("encoder", ProtoMessageEncoder())
                        .addLast("handler", TunnelServerChannelHandler(
                            tunnelRequestInterceptor = args.tunnelRequestInterceptor,
                            tunnelIds = tunnelIds,
                            tcpTunnel = tcpTunnel,
                            httpTunnel = httpTunnel,
                            httpsTunnel = httpsTunnel,
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

    private fun startSslTunnelDaemon(args: SslTunnelDaemonArgs) {
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
                        .addLast("traffic", TrafficHandler())
                        .addLast("heartbeat", HeartbeatHandler())
                        .addLast("decoder", ProtoMessageDecoder())
                        .addLast("encoder", ProtoMessageEncoder())
                        .addLast("handler", TunnelServerChannelHandler(
                            tunnelRequestInterceptor = args.tunnelRequestInterceptor,
                            tunnelIds = tunnelIds,
                            tcpTunnel = tcpTunnel,
                            httpTunnel = httpTunnel,
                            httpsTunnel = httpsTunnel,
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

    private fun createTcpTunnel(registry: TcpRegistry): TcpTunnel {
        return TcpTunnel(
            bossGroup = bossGroup,
            workerGroup = workerGroup,
            registry = registry
        )
    }

    private fun createHttpTunnel(registry: HttpRegistry, args: HttpTunnelArgs): HttpTunnel? {
        if (args.bindPort == null) return null
        return HttpTunnel(
            bossGroup = bossGroup,
            workerGroup = workerGroup,
            bindAddr = args.bindAddr,
            bindPort = args.bindPort,
            sslContext = null,
            interceptor = args.httpRequestInterceptor,
            httpPlugin = args.httpPlugin,
            registry = registry
        )
    }

    private fun createHttpsTunnel(registry: HttpRegistry, args: HttpsTunnelArgs): HttpTunnel? {
        if (args.bindPort == null) return null
        return HttpTunnel(
            bossGroup = bossGroup,
            workerGroup = workerGroup,
            bindAddr = args.bindAddr,
            bindPort = args.bindPort,
            sslContext = args.sslContext,
            interceptor = args.httpRequestInterceptor,
            httpPlugin = args.httpPlugin,
            registry = registry
        )
    }

    private fun createWebServer(args: WebServerArgs): WebServer? {
        if (args.bindPort == null) return null
        return WebServer(
            bossGroup = bossGroup,
            workerGroup = workerGroup,
            bindAddr = args.bindAddr,
            bindPort = args.bindPort
        ).apply {
            router {
                route("/api/snapshot") {
                    val obj = JSONObject()
                    obj.put("tcp", tcpRegistry.snapshot)
                    obj.put("http", httpRegistry.snapshot)
                    obj.put("https", httpsRegistry.snapshot)
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

    class TunnelDaemonArgs(
        val bindAddr: String? = null,
        val bindPort: Int = 5080,
        val tunnelRequestInterceptor: TunnelRequestInterceptor = TunnelRequestInterceptor.emptyImpl
    )

    class SslTunnelDaemonArgs(
        val bindAddr: String? = null,
        val bindPort: Int? = null,
        val tunnelRequestInterceptor: TunnelRequestInterceptor = TunnelRequestInterceptor.emptyImpl,
        val sslContext: SslContext
    )

    class HttpTunnelArgs(
        val bindAddr: String? = null,
        val bindPort: Int? = null,
        val httpRequestInterceptor: HttpRequestInterceptor = HttpRequestInterceptor.defaultImpl,
        val httpPlugin: HttpPlugin? = null
    )

    class HttpsTunnelArgs(
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