package tunnel2.client.local

import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import tunnel2.client.internal.AK_NEXT_CHANNEL
import tunnel2.client.internal.AK_SESSION_ID
import tunnel2.client.internal.AK_TUNNEL_ID
import tunnel2.common.logging.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class LocalConnector(
    workerGroup: NioEventLoopGroup
) {
    companion object {
        private val logger = LoggerFactory.getLogger(LocalConnector::class.java)
    }

    private val bootstrap = Bootstrap()
    private val cachedChannels = ConcurrentHashMap<String, Channel>()

    init {
        this.bootstrap
            .group(workerGroup)
            .channel(NioSocketChannel::class.java)
            .option(ChannelOption.AUTO_READ, true)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .handler(createChannelInitializer())
    }

    fun acquireLocalChannel(
        localAddr: String, localPort: Int,
        tunnelToken: Long, sessionToken: Long,
        tunnelClientChannel: Channel,
        callback: Callback
    ) {
        logger.trace("cachedChannels: {}", cachedChannels)
        val cachedLocalChannel = getCachedChannel(tunnelToken, sessionToken)
        if (cachedLocalChannel != null && cachedLocalChannel.isActive) {
            callback.success(cachedLocalChannel)
            return
        }
        bootstrap.connect(localAddr, localPort).addListener(ChannelFutureListener { future ->
            // 二次检查是否有可用的Channel缓存
            val localChannel = getCachedChannel(tunnelToken, sessionToken)
            if (localChannel != null && localChannel.isActive) {
                callback.success(localChannel)
                future.channel().close()
                return@ChannelFutureListener
            }
            removeLocalChannel(tunnelToken, sessionToken)
            if (future.isSuccess) {
                future.channel().attr(AK_TUNNEL_ID).set(tunnelToken)
                future.channel().attr(AK_SESSION_ID).set(sessionToken)
                future.channel().attr(AK_NEXT_CHANNEL).set(tunnelClientChannel)
                putCachedChannel(tunnelToken, sessionToken, future.channel())
                callback.success(future.channel())
            } else {
                callback.error(future.cause())
            }
        })
    }

    fun removeLocalChannel(tunnelToken: Long, sessionToken: Long): Channel? {
        return removeCachedChannel(tunnelToken, sessionToken)
    }

    fun destroy() {
        cachedChannels.clear()
    }

    private fun getCachedChannel(tunnelToken: Long, sessionToken: Long): Channel? {
        val key = getCachedChannelKey(tunnelToken, sessionToken)
        synchronized(cachedChannels) {
            return cachedChannels[key]
        }
    }

    private fun putCachedChannel(tunnelToken: Long, sessionToken: Long, channel: Channel) {
        val key = getCachedChannelKey(tunnelToken, sessionToken)
        synchronized(cachedChannels) {
            cachedChannels.put(key, channel)
        }
    }

    private fun removeCachedChannel(tunnelToken: Long, sessionToken: Long): Channel? {
        val key = getCachedChannelKey(tunnelToken, sessionToken)
        synchronized(cachedChannels) {
            return cachedChannels.remove(key)
        }
    }

    private fun getCachedChannelKey(tunnelToken: Long, sessionToken: Long): String {
        return String.format("%d-%d", tunnelToken, sessionToken)
    }

    private fun createChannelInitializer() = object : ChannelInitializer<SocketChannel>() {
        override fun initChannel(ch: SocketChannel?) {
            ch ?: return
            ch.pipeline().addLast(LocalConnectorChannelHandler(this@LocalConnector))
        }
    }

    interface Callback {
        fun success(localChannel: Channel) {}

        fun error(cause: Throwable) {}
    }

}