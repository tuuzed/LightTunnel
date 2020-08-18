package lighttunnel.client

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.ssl.SslContext
import lighttunnel.base.proto.HeartbeatHandler
import lighttunnel.base.proto.ProtoMessageDecoder
import lighttunnel.base.proto.ProtoMessageEncoder
import lighttunnel.base.util.loggerDelegate
import lighttunnel.client.conn.TunnelConnectionDefaultImpl
import lighttunnel.client.conn.TunnelConnectionRegistry
import lighttunnel.client.local.LocalTcpClient
import lighttunnel.openapi.TunnelClient.Companion.RETRY_CONNECT_POLICY_ERROR
import lighttunnel.openapi.TunnelClient.Companion.RETRY_CONNECT_POLICY_LOSE
import lighttunnel.openapi.TunnelRequest
import lighttunnel.openapi.listener.OnRemoteConnectionListener
import lighttunnel.openapi.listener.OnTunnelConnectionListener
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.experimental.and

internal class TunnelClientDaemon(
    workerThreads: Int,
    private val retryConnectPolicy: Byte,
    private val onTunnelConnectionListener: OnTunnelConnectionListener?,
    private val onRemoteConnectionListener: OnRemoteConnectionListener?
) {

    private val logger by loggerDelegate()

    private val workerGroup = if ((workerThreads >= 0)) NioEventLoopGroup(workerThreads) else NioEventLoopGroup()
    private val cachedSslBootstraps = ConcurrentHashMap<SslContext, Bootstrap>()
    private val bootstrap = Bootstrap()
    private val localTcpClient: LocalTcpClient
    private val lock = ReentrantLock()
    private val openFailureCallback = { conn: TunnelConnectionDefaultImpl -> tryReconnect(conn, false) }
    private val onChannelStateListener = OnChannelStateListenerImpl()

    val tunnelConnectionRegistry = TunnelConnectionRegistry()

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
    ): TunnelConnectionDefaultImpl {
        val conn = TunnelConnectionDefaultImpl(
            serverAddr = serverAddr,
            serverPort = serverPort,
            originalTunnelRequest = tunnelRequest,
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

    fun close(conn: TunnelConnectionDefaultImpl) {
        conn.close()
        tunnelConnectionRegistry.unregister(conn)
    }

    fun depose() = lock.withLock {
        tunnelConnectionRegistry.depose()
        cachedSslBootstraps.clear()
        localTcpClient.depose()
        workerGroup.shutdownGracefully()
        Unit
    }

    private fun tryReconnect(conn: TunnelConnectionDefaultImpl, error: Boolean) {
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
                .addLast("handler", TunnelClientDaemonChannelHandler(
                    localTcpClient = localTcpClient,
                    onChannelStateListener = onChannelStateListener,
                    onRemoteConnectListener = onRemoteConnectionListener
                ))
        }
    }

    private inner class OnChannelStateListenerImpl : TunnelClientDaemonChannelHandler.OnChannelStateListener {

        override fun onChannelInactive(
            ctx: ChannelHandlerContext,
            conn: TunnelConnectionDefaultImpl?,
            extra: TunnelClientDaemonChannelHandler.ChannelInactiveExtra?
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

        override fun onChannelConnected(ctx: ChannelHandlerContext, conn: TunnelConnectionDefaultImpl?) {
            super.onChannelConnected(ctx, conn)
            if (conn != null) {
                onTunnelConnectionListener?.onTunnelConnected(conn)
            }
        }
    }
}