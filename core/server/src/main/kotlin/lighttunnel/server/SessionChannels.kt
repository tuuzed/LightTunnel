package lighttunnel.server

import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import lighttunnel.proto.TunnelRequest
import lighttunnel.server.util.IncIds
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class SessionChannels(
    val tunnelId: Long,
    val tunnelRequest: TunnelRequest,
    val tunnelChannel: Channel
) {
    private val ids = IncIds()
    private val cachedChannels = ConcurrentHashMap<Long, Channel>()
    private val lock = ReentrantReadWriteLock()

    val cachedChannelCount: Int
        get() {
            lock.read {
                return cachedChannels.count()
            }
        }

    fun putChannel(channel: Channel): Long {
        lock.write {
            val sessionId = ids.nextId
            cachedChannels[sessionId] = channel
            return sessionId
        }
    }

    fun getChannel(sessionId: Long): Channel? {
        lock.read {
            return cachedChannels[sessionId]
        }
    }

    fun removeChannel(sessionId: Long): Channel? {
        lock.write {
            return cachedChannels.remove(sessionId)
        }
    }

    fun destroy() {
        lock.write {
            cachedChannels.forEach { (_, ch) ->
                ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
            }
            cachedChannels.clear()
        }
    }

}
