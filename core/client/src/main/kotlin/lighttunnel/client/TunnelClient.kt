@file:Suppress("CanBeParameter")

package lighttunnel.client

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslHandler
import lighttunnel.client.local.LocalTcpClient
import lighttunnel.client.util.AttributeKeys
import lighttunnel.logger.loggerDelegate
import lighttunnel.proto.HeartbeatHandler
import lighttunnel.proto.ProtoMessageDecoder
import lighttunnel.proto.ProtoMessageEncoder
import lighttunnel.proto.TunnelRequest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class TunnelClient(
    private val workerThreads: Int = -1,
    private val autoReconnect: Boolean = true,
    private val tunnelClientStateListener: OnTunnelClientStateListener? = null
) : OnConnectFailureListener, OnConnectStateListener {
    private val logger by loggerDelegate()
    private val cachedSslBootstraps = ConcurrentHashMap<SslContext, Bootstrap>()
    private val bootstrap = Bootstrap()
    private val workerGroup = if ((workerThreads >= 0)) NioEventLoopGroup(workerThreads) else NioEventLoopGroup()
    private val localTcpClient: LocalTcpClient

    override fun onConnectFailure(descriptor: TunnelConnDescriptor) {
        super.onConnectFailure(descriptor)
        if (!descriptor.isShutdown && autoReconnect) {
            // 连接失败，3秒后发起重连
            TimeUnit.SECONDS.sleep(3)
            descriptor.connect(this)
        }
    }

    override fun onChannelInactive(ctx: ChannelHandlerContext) {
        super.onChannelInactive(ctx)
        val descriptor = ctx.channel().attr(AttributeKeys.AK_TUNNEL_CONN_DESCRIPTOR).get()
        if (descriptor != null) {
            val errFlag = ctx.channel().attr(AttributeKeys.AK_ERR_FLAG).get()
            val errCause = ctx.channel().attr(AttributeKeys.AK_ERR_CAUSE).get()
            if (errFlag == true) {
                tunnelClientStateListener?.onDisconnect(descriptor, true, errCause)
                logger.trace("{}", errCause.message)
            } else {
                tunnelClientStateListener?.onDisconnect(descriptor, false, null)
                if (!descriptor.isShutdown && autoReconnect) {
                    TimeUnit.SECONDS.sleep(3)
                    descriptor.connect(this)
                    tunnelClientStateListener?.onConnecting(descriptor, true)
                }
            }
        }
    }

    override fun onTunnelConnected(ctx: ChannelHandlerContext) {
        super.onTunnelConnected(ctx)
        val descriptor = ctx.channel().attr(AttributeKeys.AK_TUNNEL_CONN_DESCRIPTOR).get()
        if (descriptor != null) {
            tunnelClientStateListener?.onConnected(descriptor)
        }
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
    ): TunnelConnDescriptor {
        val descriptor = TunnelConnDescriptor(
            if (sslContext == null) bootstrap else getSslBootstrap(sslContext),
            serverAddr,
            serverPort,
            tunnelRequest
        )
        descriptor.connect(this)
        tunnelClientStateListener?.onConnecting(descriptor, false)
        return descriptor
    }

    fun shutdown(descriptor: TunnelConnDescriptor) {
        descriptor.shutdown()
    }

    fun destroy() {
        workerGroup.shutdownGracefully()
        localTcpClient.destroy()
        cachedSslBootstraps.clear()
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
            if (sslContext != null) ch.pipeline().addFirst(SslHandler(sslContext.newEngine(ch.alloc())))
            ch.pipeline()
                .addLast(HeartbeatHandler())
                .addLast(ProtoMessageDecoder())
                .addLast(ProtoMessageEncoder())
                .addLast(TunnelClientChannelHandler(localTcpClient, this@TunnelClient))
        }
    }


}