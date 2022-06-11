package krp.krpd

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.ssl.SslContext
import io.netty.handler.timeout.IdleStateEvent
import krp.common.entity.TunnelRequest
import krp.common.exception.KrpException
import krp.common.heartbeat.HeartbeatHandler
import krp.common.proto.ProtoMsgDecoder
import krp.common.proto.ProtoMsgEncoder
import krp.common.utils.IncIds
import krp.common.utils.injectLogger
import krp.krpd.args.HttpTunnelArgs
import krp.krpd.args.HttpsTunnelArgs
import krp.krpd.args.TunnelDaemonArgs
import krp.krpd.args.TunnelSslDaemonArgs
import krp.krpd.http.HttpFd
import krp.krpd.http.HttpRegistry
import krp.krpd.http.HttpTunnel
import krp.krpd.tcp.TcpFd
import krp.krpd.tcp.TcpRegistry
import krp.krpd.tcp.TcpTunnel
import krp.krpd.traffic.TrafficHandler
import krp.krpd.utils.AK_WATCHDOG_TIME_MILLIS
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class KrpdTunnelDaemon(
    bossThreads: Int,
    workerThreads: Int,
    private val tunnelDaemonArgs: TunnelDaemonArgs?,
    private val tunnelSslDaemonArgs: TunnelSslDaemonArgs?,
    httpTunnelArgs: HttpTunnelArgs?,
    httpsTunnelArgs: HttpsTunnelArgs?,
    krpdListener: KrpdListener?,
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
    private val trafficCallback = if (krpdListener == null) null else object : TrafficHandler.Callback {
        override fun onInbound(tunnelRequest: TunnelRequest, bytes: Int) {
            krpdListener.onTrafficInbound(tunnelRequest, bytes)
        }

        override fun onOutbound(tunnelRequest: TunnelRequest, bytes: Int) {
            krpdListener.onTrafficOutbound(tunnelRequest, bytes)
        }
    }
    private val krpdTunnelDaemonCallback =
        if (krpdListener == null) null else object : KrpdTunnelDaemonChannelHandler.Callback {
            override fun onChannelConnected(ctx: ChannelHandlerContext, fd: TcpFd?) {
                fd ?: return
                krpdListener.onTcpTunnelConnected(fd)
            }

            override fun onChannelInactive(ctx: ChannelHandlerContext, fd: TcpFd?) {
                fd ?: return
                krpdListener.onTcpTunnelDisconnect(fd)
            }

            override fun onChannelConnected(ctx: ChannelHandlerContext, fd: HttpFd?) {
                fd ?: return
                krpdListener.onHttpTunnelConnected(fd)
            }

            override fun onChannelInactive(ctx: ChannelHandlerContext, fd: HttpFd?) {
                fd ?: return
                krpdListener.onHttpTunnelDisconnect(fd)
            }
        }
    private val checkHealthTimeout = TimeUnit.SECONDS.toMillis(120)
    private val checkHealthHandler = { ctx: ChannelHandlerContext, _: IdleStateEvent ->
        val watchdogTimeMillis = ctx.channel().attr(AK_WATCHDOG_TIME_MILLIS).get()
        if (watchdogTimeMillis != null && System.currentTimeMillis() - watchdogTimeMillis > checkHealthTimeout) {
            ctx.fireExceptionCaught(KrpException("heartbeat timeout"))
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
                        .addLast("handler", newKrpdTunnelDaemonChannelHandler(tunnelRequestInterceptor))
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

    private fun newKrpdTunnelDaemonChannelHandler(
        interceptor: TunnelRequestInterceptor?
    ) = KrpdTunnelDaemonChannelHandler(
        tunnelRequestInterceptor = interceptor,
        tunnelIds = tunnelIds,
        tcpTunnel = tcpTunnel,
        httpTunnel = httpTunnel,
        httpsTunnel = httpsTunnel,
        callback = krpdTunnelDaemonCallback
    )

}
