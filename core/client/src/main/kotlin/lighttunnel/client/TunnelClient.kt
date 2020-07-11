@file:Suppress("unused")

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
import lighttunnel.http.server.HttpServer
import lighttunnel.logger.loggerDelegate
import lighttunnel.proto.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.experimental.and
import kotlin.experimental.or

class TunnelClient(
    workerThreads: Int = -1,
    private val retryConnectPolicy: Byte = RETRY_CONNECT_POLICY_LOSE or RETRY_CONNECT_POLICY_ERROR,
    private val httpRpcBindAddr: String? = null,
    private val httpRpcBindPort: Int? = null,
    private val onTunnelConnectionListener: OnTunnelConnectionListener? = null,
    private val onRemoteConnectionListener: OnRemoteConnectionListener? = null
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
    private val tunnelConnectionRegistry = TunnelConnectionRegistry()
    private val lock = ReentrantLock()
    private var httpRpcServer: HttpServer? = null

    private val openFailureCallback = { conn: TunnelConnection -> tryReconnect(conn, false) }
    private val onChannelStateListener = OnChannelStateListenerImpl()

    init {
        localTcpClient = LocalTcpClient(workerGroup)
        bootstrap
            .group(workerGroup)
            .channel(NioSocketChannel::class.java)
            .option(ChannelOption.AUTO_READ, true)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .handler(InnerChannelInitializer(null))
    }

    fun connect(
        serverAddr: String,
        serverPort: Int,
        tunnelRequest: TunnelRequest,
        sslContext: SslContext? = null
    ): TunnelConnection {
        tryStartHttpRpcServer()
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
        onTunnelConnectionListener?.onTunnelConnecting(conn, false)
        tunnelConnectionRegistry.register(conn)
        return conn
    }

    fun close(conn: TunnelConnection) {
        conn.close()
        tunnelConnectionRegistry.unregister(conn)
    }

    fun getConns() = tunnelConnectionRegistry.conns

    fun depose() = lock.withLock {
        tunnelConnectionRegistry.depose()
        cachedSslBootstraps.clear()
        localTcpClient.depose()
        httpRpcServer?.depose()
        workerGroup.shutdownGracefully()
        Unit
    }

    private fun tryReconnect(conn: TunnelConnection, error: Boolean) {
        when {
            // 主动关闭
            conn.isActiveClosed -> close(conn)
            // 错误且未设置错误重连策略
            error && (retryConnectPolicy and RETRY_CONNECT_POLICY_ERROR) != RETRY_CONNECT_POLICY_ERROR -> close(conn)
            // 未设置断线重连策略
            (retryConnectPolicy and RETRY_CONNECT_POLICY_LOSE) != RETRY_CONNECT_POLICY_LOSE -> close(conn)
            else -> {
                // 连接失败，3秒后发起重连
                TimeUnit.SECONDS.sleep(3)
                conn.open(
                    bootstrap = conn.sslContext?.bootstrap ?: bootstrap,
                    failure = openFailureCallback
                )
                onTunnelConnectionListener?.onTunnelConnecting(conn, true)
            }
        }
    }

    private fun tryStartHttpRpcServer() = lock.withLock {
        if (httpRpcServer == null && httpRpcBindPort != null) {
            httpRpcServer = HttpServer(
                bossGroup = workerGroup,
                workerGroup = workerGroup,
                bindAddr = httpRpcBindAddr,
                bindPort = httpRpcBindPort
            ) {
                route("/api/snapshot") {
                    val content = Unpooled.copiedBuffer(tunnelConnectionRegistry.toJson().toString(2), Charsets.UTF_8)
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
            }.also { it.start() }
        }
    }

    private val SslContext.bootstrap: Bootstrap
        get() = cachedSslBootstraps[this] ?: Bootstrap()
            .group(workerGroup)
            .channel(NioSocketChannel::class.java)
            .option(ChannelOption.AUTO_READ, true)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .handler(InnerChannelInitializer(this))
            .also { cachedSslBootstraps[this] = it }

    private inner class InnerChannelInitializer(private val sslContext: SslContext?) : ChannelInitializer<SocketChannel>() {
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
                    onRemoteConnectListener = onRemoteConnectionListener
                ))
        }
    }

    private inner class OnChannelStateListenerImpl : TunnelClientChannelHandler.OnChannelStateListener {

        override fun onChannelInactive(
            ctx: ChannelHandlerContext,
            conn: TunnelConnection?,
            extra: TunnelClientChannelHandler.ChannelInactiveExtra?
        ) {
            super.onChannelInactive(ctx, conn, extra)
            if (conn != null) {
                onTunnelConnectionListener?.onTunnelDisconnect(conn, extra?.cause)
                logger.trace("onChannelInactive: ", extra?.cause)
                if (extra?.forceOff != true) {
                    tryReconnect(conn, extra?.cause != null)
                }
            }
        }

        override fun onChannelConnected(ctx: ChannelHandlerContext, conn: TunnelConnection?) {
            super.onChannelConnected(ctx, conn)
            if (conn != null) {
                onTunnelConnectionListener?.onTunnelConnected(conn)
            }
        }
    }

    interface OnTunnelConnectionListener {
        fun onTunnelConnecting(conn: TunnelConnection, retryConnect: Boolean) {}
        fun onTunnelConnected(conn: TunnelConnection) {}
        fun onTunnelDisconnect(conn: TunnelConnection, cause: Throwable?) {}
    }

    interface OnRemoteConnectionListener {
        fun onRemoteConnected(conn: RemoteConnection) {}
        fun onRemoteDisconnect(conn: RemoteConnection) {}
    }

}