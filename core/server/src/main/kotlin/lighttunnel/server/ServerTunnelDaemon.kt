package lighttunnel.server

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.ssl.SslContext
import io.netty.handler.timeout.IdleStateEvent
import lighttunnel.common.entity.TunnelRequest
import lighttunnel.common.exception.LightTunnelException
import lighttunnel.common.heartbeat.HeartbeatHandler
import lighttunnel.common.proto.ProtoMsgDecoder
import lighttunnel.common.proto.ProtoMsgEncoder
import lighttunnel.common.utils.IncIds
import lighttunnel.common.utils.injectLogger
import lighttunnel.server.args.HttpTunnelArgs
import lighttunnel.server.args.HttpsTunnelArgs
import lighttunnel.server.args.TunnelDaemonArgs
import lighttunnel.server.args.TunnelSslDaemonArgs
import lighttunnel.server.http.HttpDescriptor
import lighttunnel.server.http.HttpRegistry
import lighttunnel.server.http.HttpTunnel
import lighttunnel.server.tcp.TcpDescriptor
import lighttunnel.server.tcp.TcpRegistry
import lighttunnel.server.tcp.TcpTunnel
import lighttunnel.server.traffic.TrafficHandler
import lighttunnel.server.utils.AK_WATCHDOG_TIME_MILLIS
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class ServerTunnelDaemon(
    bossThreads: Int,
    workerThreads: Int,
    private val tunnelDaemonArgs: TunnelDaemonArgs?,
    private val tunnelSslDaemonArgs: TunnelSslDaemonArgs?,
    httpTunnelArgs: HttpTunnelArgs?,
    httpsTunnelArgs: HttpsTunnelArgs?,
    serverListener: ServerListener?,
) {
    private val logger by injectLogger()
    private val lock = ReentrantLock()
    private val tunnelIds = IncIds()

    private val bossGroup = if (bossThreads >= 0) NioEventLoopGroup(bossThreads) else NioEventLoopGroup()
    private val workerGroup = if (workerThreads >= 0) NioEventLoopGroup(workerThreads) else NioEventLoopGroup()

    val tcpRegistry = TcpRegistry()
    val httpRegistry = HttpRegistry()
    val httpsRegistry = HttpRegistry()

    private val tcpTunnel: TcpTunnel = newTcpTunnel(tcpRegistry)
    private val httpTunnel: HttpTunnel? = httpTunnelArgs?.let { newHttpTunnel(httpRegistry, it) }
    private val httpsTunnel: HttpTunnel? = httpsTunnelArgs?.let { newHttpsTunnel(httpsRegistry, it) }
    private val trafficCallback = if (serverListener == null) null else object : TrafficHandler.Callback {
        override fun onInbound(tunnelRequest: TunnelRequest, bytes: Int) {
            serverListener.onTrafficInbound(tunnelRequest, bytes)
        }

        override fun onOutbound(tunnelRequest: TunnelRequest, bytes: Int) {
            serverListener.onTrafficOutbound(tunnelRequest, bytes)
        }
    }
    private val serverTunnelDaemonCallback =
        if (serverListener == null) null else object : ServerTunnelDaemonChannelHandler.Callback {
            override fun onChannelConnected(ctx: ChannelHandlerContext, descriptor: TcpDescriptor?) {
                descriptor ?: return
                serverListener.onTcpTunnelConnected(descriptor)
            }

            override fun onChannelInactive(ctx: ChannelHandlerContext, descriptor: TcpDescriptor?) {
                descriptor ?: return
                serverListener.onTcpTunnelDisconnect(descriptor)
            }

            override fun onChannelConnected(ctx: ChannelHandlerContext, descriptor: HttpDescriptor?) {
                descriptor ?: return
                serverListener.onHttpTunnelConnected(descriptor)
            }

            override fun onChannelInactive(ctx: ChannelHandlerContext, descriptor: HttpDescriptor?) {
                descriptor ?: return
                serverListener.onHttpTunnelDisconnect(descriptor)
            }
        }
    private val checkHealthTimeout = TimeUnit.SECONDS.toMillis(120)
    private val checkHealthHandler = { ctx: ChannelHandlerContext, _: IdleStateEvent ->
        val watchdogTimeMillis = ctx.channel().attr(AK_WATCHDOG_TIME_MILLIS).get()
        if (watchdogTimeMillis != null && System.currentTimeMillis() - watchdogTimeMillis > checkHealthTimeout) {
            ctx.fireExceptionCaught(LightTunnelException("heartbeat timeout"))
            true
        } else {
            false
        }
    }

    @Throws(Exception::class)
    fun start(): Unit = lock.withLock {
        httpTunnel?.start()
        httpsTunnel?.start()
        tunnelDaemonArgs?.startTunnelDaemon()
        tunnelSslDaemonArgs?.startTunnelSslDaemon()
    }

    fun depose(): Unit = lock.withLock {
        tcpRegistry.depose()
        httpRegistry.depose()
        httpsRegistry.depose()
        bossGroup.shutdownGracefully()
        workerGroup.shutdownGracefully()
    }

    private fun TunnelDaemonArgs.startTunnelDaemon() =
        internalStartTunnelDaemon(
            bindIp = bindIp,
            bindPort = bindPort,
            tunnelRequestInterceptor = tunnelRequestInterceptor,
            sslContext = null,
        )

    private fun TunnelSslDaemonArgs.startTunnelSslDaemon() =
        internalStartTunnelDaemon(
            bindIp = bindIp,
            bindPort = bindPort,
            tunnelRequestInterceptor = tunnelRequestInterceptor,
            sslContext = sslContext,
        )

    private fun internalStartTunnelDaemon(
        bindIp: String?,
        bindPort: Int,
        tunnelRequestInterceptor: TunnelRequestInterceptor?,
        sslContext: SslContext?,
    ) {
        val serverBootstrap = ServerBootstrap()
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childOption(ChannelOption.AUTO_READ, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    if (sslContext != null) {
                        ch.pipeline().addFirst("ssl", sslContext.newHandler(ch.alloc()))
                    }
                    ch.pipeline()
                        .addLast("traffic", TrafficHandler(trafficCallback))
                        .addLast("heartbeat", HeartbeatHandler(allIdleTime = 15, checkHealth = checkHealthHandler))
                        .addLast("decoder", ProtoMsgDecoder())
                        .addLast("encoder", ProtoMsgEncoder())
                        .addLast("handler", newServerTunnelDaemonChannelHandler(tunnelRequestInterceptor))
                }
            })
        if (bindIp == null) {
            serverBootstrap.bind(bindPort).sync()
        } else {
            serverBootstrap.bind(bindIp, bindPort).sync()
        }
        if (sslContext == null) {
            logger.info("Serving tunnel on {} port {}", bindIp ?: "::", bindPort)
        } else {
            logger.info("Serving tunnel by ssl on {} port {}", bindIp ?: "::", bindPort)
        }
    }

    private fun newTcpTunnel(
        registry: TcpRegistry
    ): TcpTunnel {
        return TcpTunnel(
            bossGroup = bossGroup,
            workerGroup = workerGroup,
            registry = registry,
        )
    }

    private fun newHttpTunnel(
        registry: HttpRegistry, args: HttpTunnelArgs
    ): HttpTunnel {
        return HttpTunnel(
            bossGroup = bossGroup,
            workerGroup = workerGroup,
            bindIp = args.bindIp,
            bindPort = args.bindPort,
            sslContext = null,
            httpTunnelRequestInterceptor = args.httpTunnelRequestInterceptor,
            httpPlugin = args.httpPlugin,
            registry = registry,
        )
    }

    private fun newHttpsTunnel(
        registry: HttpRegistry, args: HttpsTunnelArgs
    ): HttpTunnel {
        return HttpTunnel(
            bossGroup = bossGroup,
            workerGroup = workerGroup,
            bindIp = args.bindIp,
            bindPort = args.bindPort,
            sslContext = args.sslContext,
            httpTunnelRequestInterceptor = args.httpTunnelRequestInterceptor,
            httpPlugin = args.httpPlugin,
            registry = registry
        )
    }

    private fun newServerTunnelDaemonChannelHandler(
        interceptor: TunnelRequestInterceptor?
    ) = ServerTunnelDaemonChannelHandler(
        tunnelRequestInterceptor = interceptor,
        tunnelIds = tunnelIds,
        tcpTunnel = tcpTunnel,
        httpTunnel = httpTunnel,
        httpsTunnel = httpsTunnel,
        callback = serverTunnelDaemonCallback
    )

}
