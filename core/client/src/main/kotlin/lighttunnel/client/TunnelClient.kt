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
import lighttunnel.client.conn.TunnelConnection
import lighttunnel.client.conn.TunnelConnectionRegistry
import lighttunnel.client.local.LocalTcpClient
import lighttunnel.logger.loggerDelegate
import lighttunnel.proto.*
import lighttunnel.web.server.WebServer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.experimental.and
import kotlin.experimental.or

class TunnelClient(
    private val workerThreads: Int = -1,
    private val retryConnectPolicy: Byte = RETRY_CONNECT_POLICY_LOSE or RETRY_CONNECT_POLICY_ERROR,
    private val webAddr: String? = null,
    private val webBindPort: Int? = null,
    private val onTunnelStateListener: OnTunnelStateListener? = null,
    private val onRemoteConnectListener: OnRemoteConnectListener? = null
) {

    companion object {
        const val RETRY_CONNECT_POLICY_LOSE = 0x01.toByte()  // 0000_0001
        const val RETRY_CONNECT_POLICY_ERROR = 0x02.toByte() // 0000_0010
    }

    private val logger by loggerDelegate()
    private val cachedSslBootstraps = ConcurrentHashMap<SslContext, Bootstrap>()
    private val bootstrap = Bootstrap()
    private val workerGroup = if ((workerThreads >= 0)) NioEventLoopGroup(workerThreads) else NioEventLoopGroup()
    private val localTcpClient: LocalTcpClient
    private val tunnelConnectRegistry = TunnelConnectionRegistry()
    private var webServer: WebServer? = null
    private val lock = ReentrantLock()

    private val openFailureCallback = { conn: TunnelConnection -> tryReconnect(conn, false) }
    private val onChannelStateListener = object : TunnelClientChannelHandler.OnChannelStateListener {
        override fun onChannelInactive(ctx: ChannelHandlerContext, conn: TunnelConnection?, forceOffline: Boolean, cause: Throwable?) {
            super.onChannelInactive(ctx, conn, forceOffline, cause)
            if (conn != null) {
                onTunnelStateListener?.onDisconnect(conn, cause)
                logger.trace("onChannelInactive: ", cause)
                if (!forceOffline) {
                    tryReconnect(conn, cause != null)
                }
            }
        }

        override fun onChannelConnected(ctx: ChannelHandlerContext, conn: TunnelConnection?) {
            super.onChannelConnected(ctx, conn)
            if (conn != null) {
                onTunnelStateListener?.onConnected(conn)
            }
        }
    }

    init {
        localTcpClient = LocalTcpClient(workerGroup)
        bootstrap
            .group(workerGroup)
            .channel(NioSocketChannel::class.java)
            .option(ChannelOption.AUTO_READ, true)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .handler(newChannelInitializer(null))
    }

    fun connect(
        serverAddr: String,
        serverPort: Int,
        tunnelRequest: TunnelRequest,
        sslContext: SslContext? = null
    ): TunnelConnection {
        startWebServer()
        val conn = TunnelConnection(
            serverAddr = serverAddr,
            serverPort = serverPort,
            tunnelRequest = tunnelRequest,
            sslContext = sslContext
        )
        conn.open(
            bootstrap = conn.sslContext?.bootstrap ?: bootstrap,
            failure = openFailureCallback
        )
        onTunnelStateListener?.onConnecting(conn, false)
        tunnelConnectRegistry.register(conn)
        return conn
    }

    fun close(conn: TunnelConnection) {
        conn.close()
        tunnelConnectRegistry.unregister(conn)
    }

    fun depose() = lock.withLock {
        tunnelConnectRegistry.depose()
        cachedSslBootstraps.clear()
        localTcpClient.depose()
        webServer?.depose()
        workerGroup.shutdownGracefully()
    }

    private fun tryReconnect(conn: TunnelConnection, error: Boolean) {
        if (conn.isActiveClosed) {
            // 不需要自动重连时移除缓存
            tunnelConnectRegistry.unregister(conn)
            return
        }
        if (error) {
            if ((retryConnectPolicy and RETRY_CONNECT_POLICY_ERROR) == RETRY_CONNECT_POLICY_ERROR) {
                // 连接失败，3秒后发起重连
                TimeUnit.SECONDS.sleep(3)
                conn.open(
                    bootstrap = conn.sslContext?.bootstrap ?: bootstrap,
                    failure = openFailureCallback
                )
                onTunnelStateListener?.onConnecting(conn, true)
            } else {
                // 不需要自动重连时移除缓存
                tunnelConnectRegistry.unregister(conn)
            }
        } else if ((retryConnectPolicy and RETRY_CONNECT_POLICY_LOSE) == RETRY_CONNECT_POLICY_LOSE) {
            // 连接失败，3秒后发起重连
            TimeUnit.SECONDS.sleep(3)
            conn.open(
                bootstrap = conn.sslContext?.bootstrap ?: bootstrap,
                failure = openFailureCallback
            )
            onTunnelStateListener?.onConnecting(conn, true)
        } else {
            // 不需要自动重连时移除缓存
            tunnelConnectRegistry.unregister(conn)
        }
    }

    private fun startWebServer() = lock.withLock {
        if (webServer == null && webBindPort != null) {
            val server = WebServer(
                bossGroup = workerGroup,
                workerGroup = workerGroup,
                bindAddr = webAddr,
                bindPort = webBindPort
            ).also { webServer = it }
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
            .handler(newChannelInitializer(this))
            .also { cachedSslBootstraps[this] = it }

    private fun newChannelInitializer(sslContext: SslContext?): ChannelInitializer<SocketChannel> {
        return object : ChannelInitializer<SocketChannel>() {
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
                        onChannelStateListener = onChannelStateListener,
                        onRemoteConnectListener = onRemoteConnectListener
                    ))
            }
        }
    }

    interface OnTunnelStateListener {
        fun onConnecting(conn: TunnelConnection, retryConnect: Boolean) {}
        fun onConnected(conn: TunnelConnection) {}
        fun onDisconnect(conn: TunnelConnection, cause: Throwable?) {}
    }

    interface OnRemoteConnectListener {
        fun onRemoteConnected(remoteInfo: RemoteInfo?) {}
        fun onRemoteDisconnect(remoteInfo: RemoteInfo?) {}
    }

}