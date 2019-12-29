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
import lighttunnel.api.ApiServer
import lighttunnel.client.callback.OnTunnelStateCallback
import lighttunnel.client.callback.OnTunnelStateListener
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
    private val isAutoReconnect: Boolean = true,
    private val apiServerBindAddr: String? = null,
    private val apiServerBindPort: Int = -1,
    private val onTunnelStateListener: OnTunnelStateListener? = null
) : TunnelConnectDescriptor.OnConnectFailureCallback, OnTunnelStateCallback {
    private val logger by loggerDelegate()
    private val cachedSslBootstraps = ConcurrentHashMap<SslContext, Bootstrap>()
    private val bootstrap = Bootstrap()
    private val workerGroup = if ((workerThreads >= 0)) NioEventLoopGroup(workerThreads) else NioEventLoopGroup()
    private val localTcpClient: LocalTcpClient
    private val tunnelConnectRegistry = TunnelConnectRegistry()

    private var apiBossGroup: NioEventLoopGroup? = null
    private var apiServer: ApiServer? = null

    private fun tryReconnect(descriptor: TunnelConnectDescriptor) {
        if (!descriptor.isClosed && isAutoReconnect) {
            // 连接失败，3秒后发起重连
            TimeUnit.SECONDS.sleep(3)
            descriptor.connect(this)
            onTunnelStateListener?.onConnecting(descriptor, true)
        } else {
            // 不需要自动重连时移除缓存
            tunnelConnectRegistry.unregister(descriptor)
        }
    }

    override fun onConnectFailure(descriptor: TunnelConnectDescriptor) {
        super.onConnectFailure(descriptor)
        tryReconnect(descriptor)
    }

    override fun onTunnelInactive(ctx: ChannelHandlerContext) {
        super.onTunnelInactive(ctx)
        val descriptor = ctx.channel().attr(AttributeKeys.AK_TUNNEL_CONNECT_DESCRIPTOR).get()
        if (descriptor != null) {
            val errFlag = ctx.channel().attr(AttributeKeys.AK_ERR_FLAG).get()
            val errCause = ctx.channel().attr(AttributeKeys.AK_ERR_CAUSE).get()
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

    init {
        localTcpClient = LocalTcpClient(workerGroup)
        bootstrap
            .group(workerGroup)
            .channel(NioSocketChannel::class.java)
            .option(ChannelOption.AUTO_READ, true)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .handler(createChannelInitializer(null))
    }

    @Synchronized
    fun connect(
        serverAddr: String,
        serverPort: Int,
        tunnelRequest: TunnelRequest,
        sslContext: SslContext? = null
    ): TunnelConnectDescriptor {
        val descriptor = TunnelConnectDescriptor(
            if (sslContext == null) bootstrap else getSslBootstrap(sslContext),
            serverAddr,
            serverPort,
            tunnelRequest
        )
        descriptor.connect(this)
        onTunnelStateListener?.onConnecting(descriptor, false)
        tunnelConnectRegistry.register(descriptor)
        startApiServer()
        return descriptor
    }

    @Synchronized
    fun close(descriptor: TunnelConnectDescriptor) {
        descriptor.close()
        tunnelConnectRegistry.unregister(descriptor)
    }

    @Synchronized
    fun destroy() {
        tunnelConnectRegistry.destroy()
        cachedSslBootstraps.clear()
        localTcpClient.destroy()
        apiServer?.destroy()
        apiBossGroup?.shutdownGracefully()
        workerGroup.shutdownGracefully()
    }

    private fun startApiServer() {
        if (apiServer == null && apiServerBindPort != -1) {
            val bossGroup = NioEventLoopGroup(1)
            val server = ApiServer(bossGroup, workerGroup, apiServerBindAddr, apiServerBindPort, tunnelConnectRegistry)
            server.start()
            apiBossGroup = bossGroup
            apiServer = server
            logger.info("Serving api server on {} port {}", apiServerBindAddr ?: "any address", apiServerBindPort)
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