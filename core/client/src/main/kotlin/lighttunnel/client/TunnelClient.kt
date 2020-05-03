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
import io.netty.handler.codec.http.*
import io.netty.handler.ssl.SslContext
import lighttunnel.api.server.ApiServer
import lighttunnel.client.connect.TunnelConnectFd
import lighttunnel.client.connect.TunnelConnectRegistry
import lighttunnel.client.local.LocalTcpClient
import lighttunnel.logger.loggerDelegate
import lighttunnel.proto.HeartbeatHandler
import lighttunnel.proto.ProtoMessageDecoder
import lighttunnel.proto.ProtoMessageEncoder
import lighttunnel.proto.TunnelRequest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.experimental.and
import kotlin.experimental.or

class TunnelClient(
    private val workerThreads: Int = -1,
    private val retryConnectPolicy: Byte = RETRY_CONNECT_POLICY_LOSE or RETRY_CONNECT_POLICY_ERROR,
    private val dashBindAddr: String? = null,
    private val dashboardBindPort: Int? = null,
    private val onTunnelStateListener: OnTunnelStateListener? = null
) : TunnelConnectFd.OnConnectFailureCallback, TunnelClientChannelHandler.OnChannelStateListener {

    companion object {
        const val RETRY_CONNECT_POLICY_LOSE = 0x01.toByte()  // 0000_0001
        const val RETRY_CONNECT_POLICY_ERROR = 0x02.toByte() // 0000_0010
    }

    private val logger by loggerDelegate()
    private val cachedSslBootstraps = ConcurrentHashMap<SslContext, Bootstrap>()
    private val bootstrap = Bootstrap()
    private val workerGroup = if ((workerThreads >= 0)) NioEventLoopGroup(workerThreads) else NioEventLoopGroup()
    private val localTcpClient: LocalTcpClient
    private val tunnelConnectRegistry = TunnelConnectRegistry()
    private var dashboardServer: ApiServer? = null
    private val lock = ReentrantLock()

    init {
        localTcpClient = LocalTcpClient(workerGroup)
        bootstrap
            .group(workerGroup)
            .channel(NioSocketChannel::class.java)
            .option(ChannelOption.AUTO_READ, true)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .handler(InnerChannelInitializer(localTcpClient, this))
    }

    override fun onChannelInactive(ctx: ChannelHandlerContext, fd: TunnelConnectFd?, cause: Throwable?) {
        super.onChannelInactive(ctx, fd, cause)
        if (fd != null) {
            onTunnelStateListener?.onDisconnect(fd, cause)
            logger.trace("onChannelInactive: ", cause)
            tryReconnect(fd, cause != null)
        }
    }

    override fun onChannelConnected(ctx: ChannelHandlerContext, fd: TunnelConnectFd?) {
        super.onChannelConnected(ctx, fd)
        if (fd != null) {
            onTunnelStateListener?.onConnected(fd)
        }
    }

    override fun onConnectFailure(fd: TunnelConnectFd) {
        super.onConnectFailure(fd)
        tryReconnect(fd, false)
    }

    fun connect(
        serverAddr: String,
        serverPort: Int,
        tunnelRequest: TunnelRequest,
        sslContext: SslContext? = null
    ): TunnelConnectFd {
        startDashboardServer()
        val fd = TunnelConnectFd(
            sslContext?.bootstrap ?: bootstrap,
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

    private fun tryReconnect(fd: TunnelConnectFd, error: Boolean) {
        if (fd.isActiveClosed) {
            // 不需要自动重连时移除缓存
            tunnelConnectRegistry.unregister(fd)
            return
        }
        if (error) {
            if ((retryConnectPolicy and RETRY_CONNECT_POLICY_ERROR) == RETRY_CONNECT_POLICY_ERROR) {
                // 连接失败，3秒后发起重连
                TimeUnit.SECONDS.sleep(3)
                fd.connect(this)
                onTunnelStateListener?.onConnecting(fd, true)
            } else {
                // 不需要自动重连时移除缓存
                tunnelConnectRegistry.unregister(fd)
            }
            return
        }
        if ((retryConnectPolicy and RETRY_CONNECT_POLICY_LOSE) == RETRY_CONNECT_POLICY_LOSE) {
            // 连接失败，3秒后发起重连
            TimeUnit.SECONDS.sleep(3)
            fd.connect(this)
            onTunnelStateListener?.onConnecting(fd, true)
        } else {
            // 不需要自动重连时移除缓存
            tunnelConnectRegistry.unregister(fd)
        }
    }

    private fun startDashboardServer() = lock.withLock {
        if (dashboardServer == null && dashboardBindPort != null) {
            val server = ApiServer(
                bossGroup = workerGroup,
                workerGroup = workerGroup,
                bindAddr = dashBindAddr,
                bindPort = dashboardBindPort
            ).also { dashboardServer = it }
            server.router {
                route("/api/snapshot") {
                    val content = Unpooled.copiedBuffer(tunnelConnectRegistry.snapshot.toString(2), Charsets.UTF_8)
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
            server.start()
        }
    }

    private val SslContext.bootstrap: Bootstrap
        get() = cachedSslBootstraps[this] ?: Bootstrap()
            .group(workerGroup)
            .channel(NioSocketChannel::class.java)
            .option(ChannelOption.AUTO_READ, true)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .handler(InnerChannelInitializer(
                localTcpClient = localTcpClient,
                onChannelStateListener = this@TunnelClient,
                sslContext = this
            )).also { cachedSslBootstraps[this] = it }

    private class InnerChannelInitializer(
        private val localTcpClient: LocalTcpClient,
        private val onChannelStateListener: TunnelClientChannelHandler.OnChannelStateListener,
        private val sslContext: SslContext? = null
    ) : ChannelInitializer<SocketChannel>() {
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
                    localTcpClient = localTcpClient,
                    onChannelStateListener = onChannelStateListener
                ))
        }
    }

    interface OnTunnelStateListener {
        fun onConnecting(fd: TunnelConnectFd, retryConnect: Boolean)
        fun onConnected(fd: TunnelConnectFd)
        fun onDisconnect(fd: TunnelConnectFd, cause: Throwable?)
    }
}