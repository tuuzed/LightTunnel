package lighttunnel.server

import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import lighttunnel.proto.TunnelRequest
import lighttunnel.server.util.IncIds
import java.util.concurrent.ConcurrentHashMap

class SessionChannels(
    val tunnelId: Long,
    val tunnelRequest: TunnelRequest,
    val tunnelChannel: Channel
) {
    private val ids = IncIds()
    private val cachedChannels = ConcurrentHashMap<Long, Channel>()

    val cachedChannelCount get() = syncCachedChannels { this.count() }

    fun putChannel(channel: Channel): Long {
        val sessionId = ids.nextId
        syncCachedChannels { put(sessionId, channel) }
        return sessionId
    }

    fun getChannel(sessionId: Long): Channel? = syncCachedChannels { this[sessionId] }

    fun removeChannel(sessionId: Long): Channel? = syncCachedChannels { this.remove(sessionId) }

    fun destroy() = syncCachedChannels {
        this.forEach { (_, ch) -> ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE) }
        clear()
    }


    private inline fun <R> syncCachedChannels(
        block: ConcurrentHashMap<Long, Channel>.() -> R
    ): R = synchronized(cachedChannels) { cachedChannels.block() }

}
