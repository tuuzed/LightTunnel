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
import lighttunnel.client.utils.AK_NEXT_CHANNEL
import lighttunnel.client.utils.AK_SESSION_ID
import lighttunnel.client.utils.AK_TUNNEL_ID
import java.util.concurrent.ConcurrentHashMap

internal class LocalTcpClient(workerGroup: NioEventLoopGroup) {
    private val bootstrap = Bootstrap()
    private val cachedChannels = ConcurrentHashMap<String, Channel>()

    init {
        this.bootstrap
            .group(workerGroup)
            .channel(NioSocketChannel::class.java)
            .option(ChannelOption.AUTO_READ, true)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .handler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline()
                        .addLast("handler", LocalTcpClientChannelHandler(this@LocalTcpClient))
                }
            })
    }

    fun acquireLocalChannel(
        localIp: String, localPort: Int,
        tunnelId: Long, sessionId: Long,
        tunnelClientChannel: Channel,
        callback: OnArriveLocalChannelCallback? = null
    ) {
        val checkCachedLocalChannel: (() -> Boolean) = {
            val cachedLocalChannel = getCachedChannel(tunnelId, sessionId)
            if (cachedLocalChannel?.isActive == true) {
                callback?.onArrived(cachedLocalChannel)
                true
            } else {
                false
            }
        }
        if (checkCachedLocalChannel()) {
            return
        }
        bootstrap.connect(localIp, localPort).addListener(ChannelFutureListener { future ->
            // 二次检查是否有可用的Channel缓存
            if (checkCachedLocalChannel()) {
                future.channel().close()
                return@ChannelFutureListener
            }
            removeLocalChannel(tunnelId, sessionId)
            if (future.isSuccess) {
                future.channel().attr(AK_TUNNEL_ID).set(tunnelId)
                future.channel().attr(AK_SESSION_ID).set(sessionId)
                future.channel().attr(AK_NEXT_CHANNEL).set(tunnelClientChannel)
                putCachedChannel(tunnelId, sessionId, future.channel())
                callback?.onArrived(future.channel())
            } else {
                callback?.onUnableArrive(future.cause())
            }
        })
    }

    fun removeLocalChannel(tunnelId: Long, sessionId: Long): Channel? = removeCachedChannel(tunnelId, sessionId)

    fun depose() {
        cachedChannels.values.forEach {
            it.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
        }
        cachedChannels.clear()
    }

    private fun getCachedChannel(tunnelId: Long, sessionId: Long): Channel? {
        val key = getCachedChannelKey(tunnelId, sessionId)
        return cachedChannels[key]
    }

    private fun putCachedChannel(tunnelId: Long, sessionId: Long, channel: Channel) {
        val key = getCachedChannelKey(tunnelId, sessionId)
        cachedChannels[key] = channel
    }

    private fun removeCachedChannel(tunnelId: Long, sessionId: Long): Channel? {
        val key = getCachedChannelKey(tunnelId, sessionId)
        return cachedChannels.remove(key)
    }

    private fun getCachedChannelKey(
        tunnelId: Long, sessionId: Long
    ): String = "tunnelId:$tunnelId, sessionId:$sessionId"

    interface OnArriveLocalChannelCallback {
        fun onArrived(localChannel: Channel)
        fun onUnableArrive(cause: Throwable) {}
    }

}
