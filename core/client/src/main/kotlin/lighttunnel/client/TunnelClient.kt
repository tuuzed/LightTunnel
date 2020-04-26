@file:Suppress("CanBeParameter")

package lighttunnel.client

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpVersion
import io.netty.handler.ssl.SslContext
import lighttunnel.client.callback.OnTunnelStateCallback
import lighttunnel.client.callback.OnTunnelStateListener
import lighttunnel.client.connect.TunnelConnectFd
import lighttunnel.client.connect.TunnelConnectRegistry
import lighttunnel.client.local.LocalTcpClient
import lighttunnel.client.util.AttributeKeys
import lighttunnel.dashboard.server.DashboardServer
import lighttunnel.logger.loggerDelegate
import lighttunnel.proto.HeartbeatHandler
import lighttunnel.proto.ProtoMessageDecoder
import lighttunnel.proto.ProtoMessageEncoder
import lighttunnel.proto.TunnelRequest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class TunnelClient(
    private val workerThreads: Int = -1,
    private val loseReconnect: Boolean = true,
    private val errorReconnect: Boolean = false,
    private val dashBindAddr: String? = null,
    private val dashboardBindPort: Int? = null,
    private val onTunnelStateListener: OnTunnelStateListener? = null
) : TunnelConnectFd.OnConnectFailureCallback, OnTunnelStateCallback {
    private val logger by loggerDelegate()
    private val cachedSslBootstraps = ConcurrentHashMap<SslContext, Bootstrap>()
    private val bootstrap = Bootstrap()
    private val workerGroup = if ((workerThreads >= 0)) NioEventLoopGroup(workerThreads) else NioEventLoopGroup()
    private val localTcpClient: LocalTcpClient
    private val tunnelConnectRegistry = TunnelConnectRegistry()
    private var dashboardServer: DashboardServer? = null
    private val lock = ReentrantLock()

    private fun tryReconnect(fd: TunnelConnectFd) {
        if (!fd.isClosed && (loseReconnect || errorReconnect)) {
            // 连接失败，3秒后发起重连
            TimeUnit.SECONDS.sleep(3)
            fd.connect(this)
            onTunnelStateListener?.onConnecting(fd, true)
        } else {
            // 不需要自动重连时移除缓存
            tunnelConnectRegistry.unregister(fd)
        }
    }

    override fun onTunnelInactive(ctx: ChannelHandlerContext) {
        super.onTunnelInactive(ctx)
        val fd = ctx.channel().attr(AttributeKeys.AK_TUNNEL_CONNECT_FD).get()
        if (fd != null) {
            val errFlag = ctx.channel().attr(AttributeKeys.AK_ERROR_FLAG).get()
            val errCause = ctx.channel().attr(AttributeKeys.AK_ERROR_CAUSE).get()
            if (errFlag == true) {
                onTunnelStateListener?.onDisconnect(fd, true, errCause)
                logger.trace("{}", errCause.message)
            } else {
                onTunnelStateListener?.onDisconnect(fd, false, null)
                tryReconnect(fd)
            }
        }
    }

    override fun onTunnelConnected(ctx: ChannelHandlerContext) {
        super.onTunnelConnected(ctx)
        val fd = ctx.channel().attr(AttributeKeys.AK_TUNNEL_CONNECT_FD).get()
        if (fd != null) {
            onTunnelStateListener?.onConnected(fd)
        }
    }

    override fun onConnectFailure(fd: TunnelConnectFd) {
        super.onConnectFailure(fd)
        tryReconnect(fd)
    }

    init {
        localTcpClient = LocalTcpClient(workerGroup)
        bootstrap
            .group(workerGroup)
            .channel(NioSocketChannel::class.java)
            .option(ChannelOption.AUTO_READ, true)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .handler(createChannelInitializer(null))
    }

    fun connect(
        serverAddr: String,
        serverPort: Int,
        tunnelRequest: TunnelRequest,
        sslContext: SslContext? = null
    ): TunnelConnectFd {
        startDashboardServer()
        val fd = TunnelConnectFd(
            if (sslContext == null) bootstrap else getSslBootstrap(sslContext),
            serverAddr,
            serverPort,
            tunnelRequest
        )
        fd.connect(this)
        onTunnelStateListener?.onConnecting(fd, false)
        tunnelConnectRegistry.register(fd)
        return fd
    }

    fun close(fd: TunnelConnectFd) {
        fd.close()
        tunnelConnectRegistry.unregister(fd)
    }

    fun depose() = lock.withLock {
        tunnelConnectRegistry.depose()
        cachedSslBootstraps.clear()
        localTcpClient.depose()
        dashboardServer?.depose()
        workerGroup.shutdownGracefully()
    }

    private fun startDashboardServer() = lock.withLock {
        if (dashboardServer == null && dashboardBindPort != null) {
            val server = DashboardServer(
                bossGroup = workerGroup,
                workerGroup = workerGroup,
                bindAddr = dashBindAddr,
                bindPort = dashboardBindPort
            ).router {
                route("/api/snapshot") {
                    DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1,
                        HttpResponseStatus.OK,
                        Unpooled.copiedBuffer(tunnelConnectRegistry.snapshot.toString(2), Charsets.UTF_8)
                    )
                }
            }
            dashboardServer = server
            server.start()
        }
    }

    private fun getSslBootstrap(sslContext: SslContext): Bootstrap {
        return cachedSslBootstraps[sslContext] ?: Bootstrap()
            .group(workerGroup)
            .channel(NioSocketChannel::class.java)
            .option(ChannelOption.AUTO_READ, true)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .handler(createChannelInitializer(sslContext)).also { cachedSslBootstraps[sslContext] = it }
    }

    private fun createChannelInitializer(sslContext: SslContext?) = object : ChannelInitializer<SocketChannel>() {
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
                .addLast("handler", TunnelClientChannelHandler(
                    localTcpClient, this@TunnelClient
                ))
        }
    }


}