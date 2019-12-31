package lighttunnel.server

import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import lighttunnel.proto.TunnelRequest
import lighttunnel.server.util.IncIds
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock

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
            lock.readLock().lock()
            try {
                return cachedChannels.count()
            } finally {
                lock.readLock().unlock()
            }
        }

    fun putChannel(channel: Channel): Long {
        val sessionId = ids.nextId
        lock.writeLock().lock()
        try {
            cachedChannels[sessionId] = channel
        } finally {
            lock.writeLock().unlock()
        }
        return sessionId
    }

    fun getChannel(sessionId: Long): Channel? {
        lock.readLock().lock()
        try {
            return cachedChannels[sessionId]
        } finally {
            lock.readLock().unlock()
        }
    }

    fun removeChannel(sessionId: Long): Channel? {
        lock.writeLock().lock()
        try {
            return cachedChannels.remove(sessionId)
        } finally {
            lock.writeLock().unlock()
        }
    }

    fun destroy() {
        lock.writeLock().lock()
        try {
            cachedChannels.forEach { (_, ch) ->
                ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
            }
            cachedChannels.clear()
        } finally {
            lock.writeLock().unlock()
        }
    }

}
