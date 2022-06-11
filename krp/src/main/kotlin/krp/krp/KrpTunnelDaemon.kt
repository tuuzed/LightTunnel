package krp.krp

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.ssl.SslContext
import krp.common.entity.TunnelRequest
import krp.common.heartbeat.HeartbeatHandler
import krp.common.proto.ProtoMsgDecoder
import krp.common.proto.ProtoMsgEncoder
import krp.common.utils.injectLogger
import krp.krp.Krp.Companion.RETRY_CONNECT_POLICY_ERROR
import krp.krp.Krp.Companion.RETRY_CONNECT_POLICY_LOSE
import krp.krp.conn.DefaultTunnelConn
import krp.krp.conn.TunnelConnRegistry
import krp.krp.extra.ChannelInactiveExtra
import krp.krp.local.LocalTcpClient
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.experimental.and

internal class KrpTunnelDaemon(
    workerThreads: Int,
    private val retryConnectPolicy: Byte,
    private val krpListener: KrpListener?,
) {

    private val logger by injectLogger()

    private val workerGroup = if ((workerThreads >= 0)) NioEventLoopGroup(workerThreads) else NioEventLoopGroup()
    private val cachedSslBootstraps = ConcurrentHashMap<SslContext, Bootstrap>()
    private val bootstrap = Bootstrap()
        .group(workerGroup)
        .channel(NioSocketChannel::class.java)
        .option(ChannelOption.AUTO_READ, true)
        .option(ChannelOption.SO_KEEPALIVE, true)
        .handler(newChannelInitializer(null))
    private val localTcpClient = LocalTcpClient(workerGroup)
    private val lock = ReentrantLock()
    private val openFailureCallback = { conn: DefaultTunnelConn -> tryReconnect(conn, false) }
    private val krpTunnelCallback = object : KrpTunnelDaemonChannelHandler.Callback {
        override fun onChannelInactive(
            ctx: ChannelHandlerContext, conn: DefaultTunnelConn?, extra: ChannelInactiveExtra?
        ) {
            conn ?: return
            krpListener?.onTunnelDisconnect(conn, extra?.cause)
            logger.trace("onChannelInactive: ", extra?.cause)
            if (extra?.isForceOff != true) {
                tryReconnect(conn, extra?.cause != null)
            }
        }

        override fun onChannelConnected(ctx: ChannelHandlerContext, conn: DefaultTunnelConn?) {
            conn ?: return
            krpListener?.onTunnelConnected(conn)
        }
    }
    private val tunnelConnectionRegistry = TunnelConnRegistry()

    val tunnelConnectionList get() = tunnelConnectionRegistry.tunnelConnectionList

    fun connect(
        serverIp: String,
        serverPort: Int,
        tunnelRequest: TunnelRequest,
        useEncryption: Boolean,
        sslContext: SslContext? = null,
    ): DefaultTunnelConn {
        val conn = DefaultTunnelConn(
            serverIp = serverIp,
            serverPort = serverPort,
            originalTunnelRequest = tunnelRequest,
            useEncryption = useEncryption,
            bootstrap = sslContext?.bootstrap ?: bootstrap,
        )
        conn.open(failure = openFailureCallback)
        krpListener?.onTunnelConnecting(conn, false)
        tunnelConnectionRegistry.register(conn)
        return conn
    }

    fun close(conn: DefaultTunnelConn) {
        conn.close()
        tunnelConnectionRegistry.unregister(conn)
    }

    fun depose() {
        lock.withLock {
            tunnelConnectionRegistry.depose()
            cachedSslBootstraps.clear()
            localTcpClient.depose()
            workerGroup.shutdownGracefully()
        }
    }

    private fun tryReconnect(conn: DefaultTunnelConn, error: Boolean) {
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
                conn.open(failure = openFailureCallback)
                krpListener?.onTunnelConnecting(conn, true)
            }
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

    private fun newChannelInitializer(sslContext: SslContext?) = object : ChannelInitializer<SocketChannel>() {
        override fun initChannel(ch: SocketChannel) {
            if (sslContext != null) {
                ch.pipeline()
                    .addFirst("ssl", sslContext.newHandler(ch.alloc()))
            }
            ch.pipeline()
                .addLast("heartbeat", HeartbeatHandler(allIdleTime = 30))
                .addLast("decoder", ProtoMsgDecoder())
                .addLast("encoder", ProtoMsgEncoder())
                .addLast(
                    "handler", KrpTunnelDaemonChannelHandler(
                        localTcpClient = localTcpClient,
                        callback = krpTunnelCallback,
                        krpListener = krpListener
                    )
                )
        }
    }

}
