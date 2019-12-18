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
import lighttunnel.proto.*
import lighttunnel.logging.logger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class LTClient(
    options: Options = Options()
) {
    private val logger by logger()
    private val options = options.copy()
    private val cachedSslBootstraps = ConcurrentHashMap<SslContext, Bootstrap>()
    private val bootstrap = Bootstrap()
    private val workerGroup: NioEventLoopGroup
    private val localTcpClient: LTLocalTcpClient
    private val onConnectFailureListener = object : OnConnectFailureListener {
        override fun onConnectFailure(descriptor: LTConnDescriptor) {
            super.onConnectFailure(descriptor)
            if (!descriptor.isShutdown && options.autoReconnect) {
                // 连接失败，3秒后发起重连
                TimeUnit.SECONDS.sleep(3)
                descriptor.connect(this)
            }
        }
    }
    private val tpClientChannelListener = object : OnConnectStateListener {
        override fun onChannelInactive(ctx: ChannelHandlerContext) {
            super.onChannelInactive(ctx)
            val descriptor = ctx.channel().attr<LTConnDescriptor>(AK_LT_CONN_DESCRIPTOR).get()
            if (descriptor != null) {
                val errFlag = ctx.channel().attr<Boolean>(AK_ERR_FLAG).get()
                val errCause = ctx.channel().attr<Throwable>(AK_ERR_CAUSE).get()
                if (errFlag == true) {
                    options.listener?.onDisconnect(descriptor, true, errCause)
                    logger.trace("{}", errCause.message)
                } else {
                    options.listener?.onDisconnect(descriptor, false, null)
                    if (!descriptor.isShutdown && options.autoReconnect) {
                        TimeUnit.SECONDS.sleep(3)
                        descriptor.connect(onConnectFailureListener)
                        options.listener?.onConnecting(descriptor, true)
                    }
                }
            }
        }

        override fun onTunnelConnected(ctx: ChannelHandlerContext) {
            super.onTunnelConnected(ctx)
            val descriptor = ctx.channel().attr<LTConnDescriptor>(AK_LT_CONN_DESCRIPTOR).get()
            if (descriptor != null) {
                options.listener?.onConnected(descriptor)
            }
        }
    }

    init {
        with(this.options) {
            workerGroup = if ((workerThreads >= 0)) NioEventLoopGroup(workerThreads) else NioEventLoopGroup()
            localTcpClient = LTLocalTcpClient(workerGroup)
            bootstrap
                .group(workerGroup)
                .channel(NioSocketChannel::class.java)
                .option(ChannelOption.AUTO_READ, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(createChannelInitializer(null))
        }
    }

    fun connect(
        serverAddr: String, serverPort: Int, tpRequest: LTRequest, sslContext: SslContext?
    ): LTConnDescriptor {
        val descriptor = LTConnDescriptor(
            if (sslContext == null) bootstrap else getSslBootstrap(sslContext),
            serverAddr,
            serverPort,
            tpRequest
        )
        descriptor.connect(onConnectFailureListener)
        options.listener?.onConnecting(descriptor, false)
        return descriptor
    }

    fun shutdown(descriptor: LTConnDescriptor) {
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
                .addLast(LTHeartbeatHandler())
                .addLast(LTMassageDecoder())
                .addLast(LTMassageEncoder())
                .addLast(LTClientChannelHandler(localTcpClient, tpClientChannelListener))
        }
    }


    data class Options(
        var workerThreads: Int = -1,
        var autoReconnect: Boolean = true,
        var listener: OnLTClientStateListener? = null
    )

}