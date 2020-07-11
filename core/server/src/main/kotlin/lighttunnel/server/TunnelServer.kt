@file:Suppress("unused")

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
import lighttunnel.http.server.HttpServer
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
import lighttunnel.util.SslContextUtil
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
    httpRpcServerArgs: HttpRpcServerArgs? = null,
    private val onTcpTunnelStateListener: OnTcpTunnelStateListener? = null,
    private val onHttpTunnelStateListener: OnHttpTunnelStateListener? = null
) {
    private val logger by loggerDelegate()
    private val lock = ReentrantLock()
    private val tunnelIds = IncIds()

    private val bossGroup = if (bossThreads >= 0) NioEventLoopGroup(bossThreads) else NioEventLoopGroup()
    private val workerGroup = if (workerThreads >= 0) NioEventLoopGroup(workerThreads) else NioEventLoopGroup()

    private val tcpRegistry = TcpRegistry()
    private val tcpTunnel: TcpTunnel = getTcpTunnel(tcpRegistry)

    private val httpRegistry = HttpRegistry()
    private val httpTunnel: HttpTunnel? = httpTunnelArgs?.let { getHttpTunnel(httpRegistry, it) }

    private val httpsRegistry = HttpRegistry()
    private val httpsTunnel: HttpTunnel? = httpsTunnelArgs?.let { getHttpsTunnel(httpsRegistry, it) }

    private val httpRpcServer: HttpServer? = httpRpcServerArgs?.let { getHttpRpcServer(it) }

    @Throws(Exception::class)
    fun start(): Unit = lock.withLock {
        httpTunnel?.start()
        httpsTunnel?.start()
        httpRpcServer?.start()
        startTunnelDaemon(tunnelDaemonArgs)
        sslTunnelDaemonArgs?.also { startSslTunnelDaemon(it) }
    }

    fun depose(): Unit = lock.withLock {
        tcpRegistry.depose()
        httpRegistry.depose()
        httpsRegistry.depose()
        httpRpcServer?.depose()
        bossGroup.shutdownGracefully()
        workerGroup.shutdownGracefully()
    }

    fun tcpFds() = tcpRegistry.tcpFds()
    fun httpFds() = httpRegistry.httpFds()
    fun httpsFds() = httpsRegistry.httpFds()
    fun forceOff(fd: TcpFd) = tcpRegistry.forceOff(fd.port)
    fun forceOff(fd: HttpFd, https: Boolean) = (if (https) httpsRegistry else httpRegistry).forceOff(fd.host)
    fun getTcpFdRequest(fd: TcpFd) = fd.sessionChannels.tunnelRequest
    fun getHttpFdRequest(fd: HttpFd) = fd.sessionChannels.tunnelRequest


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
                        .addLast("handler", InnerTunnelServerChannelHandler(args.tunnelRequestInterceptor))
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
                        .addLast("handler", InnerTunnelServerChannelHandler(args.tunnelRequestInterceptor))
                }
            })
        if (args.bindAddr == null) {
            serverBootstrap.bind(args.bindPort).get()
        } else {
            serverBootstrap.bind(args.bindAddr, args.bindPort).get()
        }
        logger.info("Serving tunnel with ssl on {} port {}", args.bindAddr ?: "::", args.bindPort)
    }

    private fun getTcpTunnel(registry: TcpRegistry): TcpTunnel {
        return TcpTunnel(
            bossGroup = bossGroup,
            workerGroup = workerGroup,
            registry = registry
        )
    }

    private fun getHttpTunnel(registry: HttpRegistry, args: HttpTunnelArgs): HttpTunnel? {
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

    private fun getHttpsTunnel(registry: HttpRegistry, args: HttpsTunnelArgs): HttpTunnel? {
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

    private fun getHttpRpcServer(args: HttpRpcServerArgs): HttpServer? {
        if (args.bindPort == null) return null
        return HttpServer(
            bossGroup = bossGroup,
            workerGroup = workerGroup,
            bindAddr = args.bindAddr,
            bindPort = args.bindPort
        ) {
            route("/api/snapshot") {
                val obj = JSONObject()
                obj.put("tcp", tcpRegistry.toJson())
                obj.put("http", httpRegistry.toJson())
                obj.put("https", httpsRegistry.toJson())
                val content = Unpooled.copiedBuffer(obj.toString(2), Charsets.UTF_8)
                DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK,
                    content
                ).apply {
                    headers()
                        .set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                        .set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes())
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
        val sslContext: SslContext = SslContextUtil.forBuiltinServer()
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
        val sslContext: SslContext = SslContextUtil.forBuiltinServer()
    )

    class HttpRpcServerArgs(
        val bindAddr: String? = null,
        val bindPort: Int? = null
    )

    private inner class InnerTunnelServerChannelHandler(tunnelRequestInterceptor: TunnelRequestInterceptor) : TunnelServerChannelHandler(
        tunnelRequestInterceptor = tunnelRequestInterceptor,
        tunnelIds = tunnelIds,
        tcpTunnel = tcpTunnel,
        httpTunnel = httpTunnel,
        httpsTunnel = httpsTunnel
    ) {
        override fun onChannelConnected(ctx: ChannelHandlerContext, tcpFd: TcpFd?) {
            if (tcpFd != null) {
                onTcpTunnelStateListener?.onTcpTunnelConnected(tcpFd)
            }
        }

        override fun onChannelInactive(ctx: ChannelHandlerContext, tcpFd: TcpFd?) {
            if (tcpFd != null) {
                onTcpTunnelStateListener?.onTcpTunnelDisconnect(tcpFd)
            }
        }

        override fun onChannelConnected(ctx: ChannelHandlerContext, httpFd: HttpFd?) {
            if (httpFd != null) {
                onHttpTunnelStateListener?.onHttpTunnelConnected(httpFd)
            }
        }

        override fun onChannelInactive(ctx: ChannelHandlerContext, httpFd: HttpFd?) {
            if (httpFd != null) {
                onHttpTunnelStateListener?.onHttpTunnelDisconnect(httpFd)
            }
        }
    }

    interface OnTcpTunnelStateListener {
        fun onTcpTunnelConnected(fd: TcpFd) {}
        fun onTcpTunnelDisconnect(fd: TcpFd) {}
    }

    interface OnHttpTunnelStateListener {
        fun onHttpTunnelConnected(fd: HttpFd) {}
        fun onHttpTunnelDisconnect(fd: HttpFd) {}
    }


}