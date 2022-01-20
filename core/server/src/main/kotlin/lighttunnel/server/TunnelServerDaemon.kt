package lighttunnel.server

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import lighttunnel.base.heartbeat.HeartbeatHandler
import lighttunnel.base.proto.ProtoMsgDecoder
import lighttunnel.base.proto.ProtoMsgEncoder
import lighttunnel.base.utils.IncIds
import lighttunnel.base.utils.loggerDelegate
import lighttunnel.server.args.HttpTunnelArgs
import lighttunnel.server.args.HttpsTunnelArgs
import lighttunnel.server.args.SslTunnelDaemonArgs
import lighttunnel.server.args.TunnelDaemonArgs
import lighttunnel.server.http.HttpFd
import lighttunnel.server.http.HttpRegistry
import lighttunnel.server.http.HttpTunnel
import lighttunnel.server.listener.OnHttpTunnelStateListener
import lighttunnel.server.listener.OnTcpTunnelStateListener
import lighttunnel.server.listener.OnTrafficListener
import lighttunnel.server.tcp.TcpFd
import lighttunnel.server.tcp.TcpRegistry
import lighttunnel.server.tcp.TcpTunnel
import lighttunnel.server.traffic.TrafficHandler
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class TunnelServerDaemon(
    bossThreads: Int,
    workerThreads: Int,
    private val tunnelDaemonArgs: TunnelDaemonArgs?,
    private val sslTunnelDaemonArgs: SslTunnelDaemonArgs?,
    httpTunnelArgs: HttpTunnelArgs?,
    httpsTunnelArgs: HttpsTunnelArgs?,
    private val onTcpTunnelStateListener: OnTcpTunnelStateListener?,
    private val onHttpTunnelStateListener: OnHttpTunnelStateListener?,
    private val onTrafficListener: OnTrafficListener?,
    // Http和Https共享Registry
    isHttpAndHttpsShareRegistry: Boolean = false
) {
    private val logger by loggerDelegate()
    private val lock = ReentrantLock()
    private val tunnelIds = IncIds()

    private val bossGroup = if (bossThreads >= 0) NioEventLoopGroup(bossThreads) else NioEventLoopGroup()
    private val workerGroup = if (workerThreads >= 0) NioEventLoopGroup(workerThreads) else NioEventLoopGroup()

    val tcpRegistry: TcpRegistry = TcpRegistry()
    val httpRegistry: HttpRegistry = HttpRegistry()
    val httpsRegistry: HttpRegistry = if (isHttpAndHttpsShareRegistry) httpRegistry else HttpRegistry()

    private val tcpTunnel: TcpTunnel = getTcpTunnel(tcpRegistry)
    private val httpTunnel: HttpTunnel? = httpTunnelArgs?.let { getHttpTunnel(httpRegistry, it) }
    private val httpsTunnel: HttpTunnel? = httpsTunnelArgs?.let { getHttpsTunnel(httpsRegistry, it) }

    @Throws(Exception::class)
    fun start(): Unit = lock.withLock {
        httpTunnel?.start()
        httpsTunnel?.start()
        tunnelDaemonArgs?.let { startTunnelDaemon(it) }
        sslTunnelDaemonArgs?.also { startSslTunnelDaemon(it) }
    }

    fun depose(): Unit = lock.withLock {
        tcpRegistry.depose()
        httpRegistry.depose()
        httpsRegistry.depose()
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
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline()
                        .addLast("traffic", TrafficHandler(onTrafficListener))
                        .addLast("heartbeat", HeartbeatHandler(15, 0))
                        .addLast("decoder", ProtoMsgDecoder())
                        .addLast("encoder", ProtoMsgEncoder())
                        .addLast("handler", newTunnelServerChannelHandler(args.tunnelRequestInterceptor))
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
        val serverBootstrap = ServerBootstrap()
        serverBootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childOption(ChannelOption.AUTO_READ, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline()
                        .addFirst("ssl", args.sslContext.newHandler(ch.alloc()))
                        .addLast("traffic", TrafficHandler(onTrafficListener))
                        .addLast("heartbeat", HeartbeatHandler(15, 0))
                        .addLast("decoder", ProtoMsgDecoder())
                        .addLast("encoder", ProtoMsgEncoder())
                        .addLast("handler", newTunnelServerChannelHandler(args.tunnelRequestInterceptor))
                }
            })
        if (args.bindAddr == null) {
            serverBootstrap.bind(args.bindPort).get()
        } else {
            serverBootstrap.bind(args.bindAddr, args.bindPort).get()
        }
        logger.info("Serving tunnel by ssl on {} port {}", args.bindAddr ?: "::", args.bindPort)
    }

    private fun getTcpTunnel(registry: TcpRegistry): TcpTunnel {
        return TcpTunnel(
            bossGroup = bossGroup,
            workerGroup = workerGroup,
            registry = registry
        )
    }

    private fun getHttpTunnel(registry: HttpRegistry, args: HttpTunnelArgs): HttpTunnel {
        return HttpTunnel(
            bossGroup = bossGroup,
            workerGroup = workerGroup,
            bindAddr = args.bindAddr,
            bindPort = args.bindPort,
            sslContext = null,
            httpTunnelRequestInterceptor = args.httpTunnelRequestInterceptor,
            httpPlugin = args.httpPlugin,
            registry = registry
        )
    }

    private fun getHttpsTunnel(registry: HttpRegistry, args: HttpsTunnelArgs): HttpTunnel {
        return HttpTunnel(
            bossGroup = bossGroup,
            workerGroup = workerGroup,
            bindAddr = args.bindAddr,
            bindPort = args.bindPort,
            sslContext = args.sslContext,
            httpTunnelRequestInterceptor = args.httpTunnelRequestInterceptor,
            httpPlugin = args.httpPlugin,
            registry = registry
        )
    }

    private fun newTunnelServerChannelHandler(
        tunnelRequestInterceptor: TunnelRequestInterceptor?
    ) = TunnelServerDaemonChannelHandler(
        tunnelRequestInterceptor = tunnelRequestInterceptor,
        tunnelIds = tunnelIds,
        tcpTunnel = tcpTunnel,
        httpTunnel = httpTunnel,
        httpsTunnel = httpsTunnel,
        callback = object : TunnelServerDaemonChannelHandler.Callback {
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
    )

}
