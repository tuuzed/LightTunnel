package lighttunnel.server.util

import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import lighttunnel.proto.TunnelRequest
import lighttunnel.util.IncIds
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class SessionChannels(
    val tunnelId: Long,
    val tunnelRequest: TunnelRequest,
    val tunnelChannel: Channel
) {
    private val ids = IncIds()
    private val cachedChannels = HashMap<Long, Channel>()
    private val lock = ReentrantReadWriteLock()

    val cachedChannelCount: Int get() = cachedChannels.count()

    fun putChannel(channel: Channel): Long {
        val sessionId = ids.nextId
        lock.write { cachedChannels[sessionId] = channel }
        return sessionId
    }

    fun getChannel(sessionId: Long): Channel? = lock.read { cachedChannels[sessionId] }

    fun removeChannel(sessionId: Long): Channel? = lock.write { cachedChannels.remove(sessionId) }

    fun destroy() = lock.write {
        cachedChannels.forEach { (_, ch) ->
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
        }
        cachedChannels.clear()
        Unit
    }
}
