package lighttunnel.client.local

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import lighttunnel.client.util.AttributeKeys
import lighttunnel.logger.loggerDelegate
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class LocalTcpClient(
    workerGroup: NioEventLoopGroup
) {
    private val logger by loggerDelegate()
    private val bootstrap = Bootstrap()
    private val cachedChannels = hashMapOf<String, Channel>()
    private val lock = ReentrantReadWriteLock()

    init {
        this.bootstrap
            .group(workerGroup)
            .channel(NioSocketChannel::class.java)
            .option(ChannelOption.AUTO_READ, true)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .handler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel?) {
                    ch ?: return
                    ch.pipeline()
                        .addLast("handler", LocalTcpClientChannelHandler(this@LocalTcpClient))
                }
            })
    }

    fun withLocalChannel(
        localAddr: String, localPort: Int,
        tunnelId: Long, sessionId: Long,
        tunnelClientChannel: Channel,
        callback: OnArriveLocalChannelCallback? = null
    ) {
        logger.trace("cachedChannels: {}", cachedChannels)
        val cachedLocalChannel = getCachedChannel(tunnelId, sessionId)
        if (cachedLocalChannel != null && cachedLocalChannel.isActive) {
            callback?.onArrived(cachedLocalChannel)
            return
        }
        bootstrap.connect(localAddr, localPort).addListener(ChannelFutureListener { future ->
            // 二次检查是否有可用的Channel缓存
            val localChannel = getCachedChannel(tunnelId, sessionId)
            if (localChannel != null && localChannel.isActive) {
                callback?.onArrived(localChannel)
                future.channel().close()
                return@ChannelFutureListener
            }
            removeLocalChannel(tunnelId, sessionId)
            if (future.isSuccess) {
                future.channel().attr(AttributeKeys.AK_TUNNEL_ID).set(tunnelId)
                future.channel().attr(AttributeKeys.AK_SESSION_ID).set(sessionId)
                future.channel().attr(AttributeKeys.AK_NEXT_CHANNEL).set(tunnelClientChannel)
                putCachedChannel(tunnelId, sessionId, future.channel())
                callback?.onArrived(future.channel())
            } else {
                callback?.onUnableArrive(future.cause())
            }
        })
    }

    fun removeLocalChannel(tunnelId: Long, sessionId: Long): Channel? {
        return removeCachedChannel(tunnelId, sessionId)
    }

    fun depose() = lock.write {
        cachedChannels.values.forEach {
            it.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
        }
        cachedChannels.clear()
        Unit
    }

    private fun getCachedChannel(tunnelId: Long, sessionId: Long): Channel? {
        val key = getCachedChannelKey(tunnelId, sessionId)
        return lock.read { cachedChannels[key] }
    }

    private fun putCachedChannel(tunnelId: Long, sessionId: Long, channel: Channel) {
        val key = getCachedChannelKey(tunnelId, sessionId)
        lock.write { cachedChannels.put(key, channel) }
    }

    private fun removeCachedChannel(tunnelId: Long, sessionId: Long): Channel? {
        val key = getCachedChannelKey(tunnelId, sessionId)
        return lock.write { cachedChannels.remove(key) }
    }

    private fun getCachedChannelKey(tunnelId: Long, sessionId: Long): String = "tunnelId:$tunnelId, sessionId:$sessionId"

    interface OnArriveLocalChannelCallback {
        fun onArrived(localChannel: Channel)
        fun onUnableArrive(cause: Throwable) {}
    }

}