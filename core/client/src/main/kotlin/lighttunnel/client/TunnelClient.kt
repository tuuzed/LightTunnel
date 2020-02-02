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
import lighttunnel.client.connect.TunnelConnectDescriptor
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
) : TunnelConnectDescriptor.OnConnectFailureCallback, OnTunnelStateCallback {
    private val logger by loggerDelegate()
    private val cachedSslBootstraps = ConcurrentHashMap<SslContext, Bootstrap>()
    private val bootstrap = Bootstrap()
    private val workerGroup = if ((workerThreads >= 0)) NioEventLoopGroup(workerThreads) else NioEventLoopGroup()
    private val localTcpClient: LocalTcpClient
    private val tunnelConnectRegistry = TunnelConnectRegistry()
    private var dashboardServer: DashboardServer? = null
    private val lock = ReentrantLock()

    private fun tryReconnect(descriptor: TunnelConnectDescriptor) {
        if (!descriptor.isClosed && (loseReconnect || errorReconnect)) {
            // 连接失败，3秒后发起重连
            TimeUnit.SECONDS.sleep(3)
            descriptor.connect(this)
            onTunnelStateListener?.onConnecting(descriptor, true)
        } else {
            // 不需要自动重连时移除缓存
            tunnelConnectRegistry.unregister(descriptor)
        }
    }

    override fun onTunnelInactive(ctx: ChannelHandlerContext) {
        super.onTunnelInactive(ctx)
        val descriptor = ctx.channel().attr(AttributeKeys.AK_TUNNEL_CONNECT_DESCRIPTOR).get()
        if (descriptor != null) {
            val errFlag = ctx.channel().attr(AttributeKeys.AK_ERROR_FLAG).get()
            val errCause = ctx.channel().attr(AttributeKeys.AK_ERROR_CAUSE).get()
            if (errFlag == true) {
                onTunnelStateListener?.onDisconnect(descriptor, true, errCause)
                logger.trace("{}", errCause.message)
            } else {
                onTunnelStateListener?.onDisconnect(descriptor, false, null)
                tryReconnect(descriptor)
            }
        }
    }

    override fun onTunnelConnected(ctx: ChannelHandlerContext) {
        super.onTunnelConnected(ctx)
        val descriptor = ctx.channel().attr(AttributeKeys.AK_TUNNEL_CONNECT_DESCRIPTOR).get()
        if (descriptor != null) {
            onTunnelStateListener?.onConnected(descriptor)
        }
    }

    override fun onConnectFailure(descriptor: TunnelConnectDescriptor) {
        super.onConnectFailure(descriptor)
        tryReconnect(descriptor)
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
    ): TunnelConnectDescriptor {
        startDashboardServer()
        val descriptor = TunnelConnectDescriptor(
            if (sslContext == null) bootstrap else getSslBootstrap(sslContext),
            serverAddr,
            serverPort,
            tunnelRequest
        )
        descriptor.connect(this)
        onTunnelStateListener?.onConnecting(descriptor, false)
        tunnelConnectRegistry.register(descriptor)
        return descriptor
    }

    fun close(descriptor: TunnelConnectDescriptor) {
        descriptor.close()
        tunnelConnectRegistry.unregister(descriptor)
    }

    fun destroy() = lock.withLock {
        tunnelConnectRegistry.destroy()
        cachedSslBootstraps.clear()
        localTcpClient.destroy()
        dashboardServer?.destroy()
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