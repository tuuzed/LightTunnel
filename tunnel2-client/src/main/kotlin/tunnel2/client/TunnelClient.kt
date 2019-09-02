package tunnel2.client

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslHandler
import tunnel2.client.internal.AK_ERR_CAUSE
import tunnel2.client.internal.AK_ERR_FLAG
import tunnel2.client.internal.AK_TUNNEL_CLIENT_DESCRIPTOR
import tunnel2.client.local.LocalConnector
import tunnel2.common.TunnelHeartbeatHandler
import tunnel2.common.TunnelRequest
import tunnel2.common.logging.LoggerFactory
import tunnel2.common.proto.ProtoMessageDecoder
import tunnel2.common.proto.ProtoMessageEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit


class TunnelClient(
    workerThreads: Int = -1,
    private val autoReconnect: Boolean = true,
    private val listener: Listener? = null
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TunnelClient::class.java)
    }

    private val cachedSslBootstraps = ConcurrentHashMap<SslContext, Bootstrap>()
    private val connectFailureCallback = object : TunnelClientDescriptor.ConnectFailureCallback {
        override fun invoke(descriptor: TunnelClientDescriptor) {
            if (!descriptor.isShutdown() && autoReconnect) {
                // 连接失败，3秒后发起重连
                TimeUnit.SECONDS.sleep(3)
                descriptor.connect(this)
            }
        }
    }
    private val bootstrap = Bootstrap()
    private val workerGroup = if ((workerThreads >= 0)) NioEventLoopGroup(workerThreads) else NioEventLoopGroup()
    private val tunnelClientChannelListener = object : TunnelClientChannelHandler.Listener {
        override fun channelInactive(ctx: ChannelHandlerContext) {
            val descriptor = ctx.channel().attr<TunnelClientDescriptor>(AK_TUNNEL_CLIENT_DESCRIPTOR).get()
            if (descriptor != null) {
                val fatalFlag = ctx.channel().attr<Boolean>(AK_ERR_FLAG).get()
                val fatalCause = ctx.channel().attr<Throwable>(AK_ERR_CAUSE).get()
                if (fatalFlag != null && fatalFlag) {
                    listener?.onDisconnect(descriptor, true)
                    logger.debug("{}", fatalCause.message, fatalCause)
                } else {
                    listener?.onDisconnect(descriptor, false)
                    if (!descriptor.isShutdown() && autoReconnect) {
                        TimeUnit.SECONDS.sleep(3)
                        descriptor.connect(connectFailureCallback)
                        listener?.onConnecting(descriptor, true)
                    }
                }
            }
        }

        override fun tunnelConnected(ctx: ChannelHandlerContext) {
            val descriptor = ctx.channel().attr<TunnelClientDescriptor>(AK_TUNNEL_CLIENT_DESCRIPTOR).get()
            if (descriptor != null) {
                listener?.onConnected(descriptor)
            }
        }
    }
    private val localConnector = LocalConnector(workerGroup)

    init {
        bootstrap
            .group(workerGroup)
            .channel(NioSocketChannel::class.java)
            .option(ChannelOption.AUTO_READ, true)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .handler(createChannelInitializer(null))
    }

    fun connect(
        serverAddr: String, serverPort: Int,
        tunnelRequest: TunnelRequest,
        sslContext: SslContext?
    ): TunnelClientDescriptor {

        val descriptor = if (sslContext == null)
            TunnelClientDescriptor(bootstrap, serverAddr, serverPort, tunnelRequest)
        else
            TunnelClientDescriptor(getSslBootstrap(sslContext), serverAddr, serverPort, tunnelRequest)
        descriptor.connect(connectFailureCallback)
        listener?.onConnecting(descriptor, false)
        return descriptor
    }

    fun shutdown(descriptor: TunnelClientDescriptor) {
        descriptor.shutdown()
    }

    fun destroy() {
        workerGroup.shutdownGracefully()
        localConnector.destroy()
        cachedSslBootstraps.clear()
    }

    private fun getSslBootstrap(sslContext: SslContext): Bootstrap {
        return cachedSslBootstraps[sslContext] ?: Bootstrap()
            .group(workerGroup)
            .channel(NioSocketChannel::class.java)
            .option(ChannelOption.AUTO_READ, true)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .handler(createChannelInitializer(sslContext)).also {
                cachedSslBootstraps[sslContext] = it
            }
    }


    private fun createChannelInitializer(sslContext: SslContext?) = object : ChannelInitializer<SocketChannel>() {
        override fun initChannel(ch: SocketChannel?) {
            ch ?: return
            if (sslContext != null) {
                ch.pipeline().addFirst(
                    SslHandler(sslContext.newEngine(ch.alloc()))
                )
            }
            ch.pipeline()
                .addLast(ProtoMessageDecoder())
                .addLast(ProtoMessageEncoder())
                .addLast(TunnelHeartbeatHandler())
                .addLast(TunnelClientChannelHandler(
                    localConnector,
                    tunnelClientChannelListener
                ))
        }
    }

    interface Listener {
        fun onConnecting(descriptor: TunnelClientDescriptor, reconnect: Boolean) {}
        fun onConnected(descriptor: TunnelClientDescriptor) {}
        fun onDisconnect(descriptor: TunnelClientDescriptor, err: Boolean) {}
    }

}